package com.supera.desafio.access.service;

import com.supera.desafio.access.dto.AccessRequestCancelRequest;
import com.supera.desafio.access.dto.AccessRequestCreateRequest;
import com.supera.desafio.access.dto.AccessRequestHistoryDto;
import com.supera.desafio.access.dto.AccessRequestResponse;
import com.supera.desafio.access.dto.ModuleDto;
import com.supera.desafio.access.dto.PageResponse;
import com.supera.desafio.core.domain.history.AccessRequestHistory;
import com.supera.desafio.core.domain.history.AccessRequestHistoryEventType;
import com.supera.desafio.core.domain.module.ModuleAccess;
import com.supera.desafio.core.domain.module.ModuleAccessStatus;
import com.supera.desafio.core.domain.module.SystemModule;
import com.supera.desafio.core.domain.request.AccessRequest;
import com.supera.desafio.core.domain.request.AccessRequestStatus;
import com.supera.desafio.core.domain.user.SystemUser;
import com.supera.desafio.core.repository.AccessRequestRepository;
import com.supera.desafio.core.repository.ModuleAccessRepository;
import com.supera.desafio.core.repository.SystemModuleRepository;
import com.supera.desafio.core.repository.SystemUserRepository;
import com.supera.desafio.security.model.AuthenticatedUser;
import com.supera.desafio.security.service.CurrentUserService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessRequestService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final Set<String> GENERIC_JUSTIFICATIONS = Set.of("teste", "aaaa", "aaa", "preciso");

    private final AccessRequestRepository accessRequestRepository;
    private final SystemModuleRepository moduleRepository;
    private final ModuleAccessRepository moduleAccessRepository;
    private final SystemUserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AccessRequestService(AccessRequestRepository accessRequestRepository,
                                SystemModuleRepository moduleRepository,
                                ModuleAccessRepository moduleAccessRepository,
                                SystemUserRepository userRepository,
                                CurrentUserService currentUserService) {
        this.accessRequestRepository = accessRequestRepository;
        this.moduleRepository = moduleRepository;
        this.moduleAccessRepository = moduleAccessRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AccessRequestResponse create(AccessRequestCreateRequest payload) {
        AuthenticatedUser current = currentUserService.requireUser();
        SystemUser requester = userRepository.findById(current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        validateJustification(payload.justification());

        List<SystemModule> modules = moduleRepository.findAllById(payload.moduleIds());
        if (modules.size() != payload.moduleIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Módulo inválido informado");
        }
        if (modules.stream().anyMatch(module -> !module.isActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos os módulos devem estar ativos");
        }

        validateDepartmentPermissions(requester, modules);
        validateNoDuplicateRequests(current.id(), modules);
        validateNoActiveAccess(current.id(), modules);
        validateMutualExclusion(current.id(), modules);
        validateAccessLimit(current.id(), requester, modules.size());

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setRequester(requester);
        accessRequest.setRequesterDepartment(requester.getDepartment());
        accessRequest.setJustification(payload.justification());
        accessRequest.setUrgent(payload.urgent());
        accessRequest.setStatus(AccessRequestStatus.ATIVO);
        accessRequest.setProtocol(generateProtocol());
        accessRequest.setRequestedAt(Instant.now());
        accessRequest.setExpiresAt(accessRequest.getRequestedAt().plus(180, ChronoUnit.DAYS));
        accessRequest.getModules().addAll(modules);
        addHistory(accessRequest, AccessRequestHistoryEventType.CRIADO, "Solicitação criada");
        addHistory(accessRequest, AccessRequestHistoryEventType.APROVADO, "Acesso concedido automaticamente");

        AccessRequest saved = accessRequestRepository.save(accessRequest);

        modules.forEach(module -> {
            ModuleAccess moduleAccess = new ModuleAccess();
            moduleAccess.setModule(module);
            moduleAccess.setUser(requester);
            moduleAccess.setStatus(ModuleAccessStatus.ATIVO);
            moduleAccess.setGrantedAt(saved.getRequestedAt());
            moduleAccess.setExpiresAt(saved.getExpiresAt());
            moduleAccessRepository.save(moduleAccess);
        });

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccessRequestResponse> list(String q,
                                                    AccessRequestStatus status,
                                                    Instant start,
                                                    Instant end,
                                                    Boolean urgent,
                                                    int page,
                                                    Integer sizeParam) {
        AuthenticatedUser current = currentUserService.requireUser();
        int size = sizeParam == null ? DEFAULT_PAGE_SIZE : Math.min(sizeParam, DEFAULT_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        Specification<AccessRequest> specification = AccessRequestSpecifications.belongsTo(current.id());
        if (status != null) {
            specification = specification.and(AccessRequestSpecifications.hasStatus(status));
        }
        if (start != null && end != null) {
            specification = specification.and(AccessRequestSpecifications.betweenDates(start, end));
        }
        if (urgent != null) {
            specification = specification.and(AccessRequestSpecifications.urgent(urgent));
        }
        if (q != null && !q.isBlank()) {
            specification = specification.and(AccessRequestSpecifications.matchesText(q));
        }
        Page<AccessRequest> pageData = accessRequestRepository.findAll(specification, pageable);
        List<AccessRequestResponse> content = pageData.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, pageData.getNumber(), pageData.getSize(), pageData.getTotalElements(), pageData.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AccessRequestResponse detail(Long id) {
        AuthenticatedUser current = currentUserService.requireUser();
        AccessRequest request = accessRequestRepository.findByIdAndRequesterId(id, current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        return toResponse(request);
    }

    @Transactional
    public AccessRequestResponse cancel(Long id, AccessRequestCancelRequest payload) {
        AuthenticatedUser current = currentUserService.requireUser();
        AccessRequest accessRequest = accessRequestRepository.findByIdAndRequesterId(id, current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        if (accessRequest.getStatus() != AccessRequestStatus.ATIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente solicitações ativas podem ser canceladas");
        }
        Instant now = Instant.now();
        accessRequest.setStatus(AccessRequestStatus.CANCELADO);
        accessRequest.setDeniedReason(payload.reason());
        addHistory(accessRequest, AccessRequestHistoryEventType.CANCELADO, "Cancelada: " + payload.reason());

        Set<Long> moduleIds = accessRequest.getModules().stream()
                .map(SystemModule::getId)
                .collect(Collectors.toSet());
        moduleIds.forEach(moduleId -> moduleAccessRepository.findByUserIdAndModuleIdAndStatus(current.id(), moduleId, ModuleAccessStatus.ATIVO)
                .ifPresent(access -> {
                    access.setStatus(ModuleAccessStatus.REVOGADO);
                    access.setRevokedAt(now);
                    moduleAccessRepository.save(access);
                }));

        return toResponse(accessRequest);
    }

    @Transactional
    public AccessRequestResponse renew(Long id) {
        AuthenticatedUser current = currentUserService.requireUser();
        AccessRequest original = accessRequestRepository.findByIdAndRequesterId(id, current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        if (original.getStatus() != AccessRequestStatus.ATIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente solicitações ativas podem ser renovadas");
        }
        Instant now = Instant.now();
        if (original.getExpiresAt() == null || ChronoUnit.DAYS.between(now, original.getExpiresAt()) > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renovação permitida apenas faltando 30 dias ou menos");
        }

        AccessRequest renewal = new AccessRequest();
        renewal.setParentRequest(original);
        renewal.setRequester(original.getRequester());
        renewal.setRequesterDepartment(original.getRequesterDepartment());
        renewal.setJustification("Renovação automática da solicitação " + original.getProtocol());
        renewal.setUrgent(false);
        renewal.setStatus(AccessRequestStatus.ATIVO);
        renewal.setProtocol(generateProtocol());
        renewal.setRequestedAt(now);
        renewal.setExpiresAt(now.plus(180, ChronoUnit.DAYS));
        renewal.getModules().addAll(original.getModules());
        addHistory(renewal, AccessRequestHistoryEventType.RENOVADO, "Solicitação renovada a partir de " + original.getProtocol());

        AccessRequest saved = accessRequestRepository.save(renewal);

        original.getModules().forEach(module -> moduleAccessRepository.findByUserIdAndModuleIdAndStatus(current.id(), module.getId(), ModuleAccessStatus.ATIVO)
                .ifPresent(access -> {
                    access.setExpiresAt(saved.getExpiresAt());
                    moduleAccessRepository.save(access);
                }));

        addHistory(original, AccessRequestHistoryEventType.RENOVADO, "Renovada em " + saved.getProtocol());

        return toResponse(saved);
    }

    private void validateJustification(String justification) {
        String normalized = justification.replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);
        if (normalized.length() <= 3 || GENERIC_JUSTIFICATIONS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Justificativa insuficiente ou genérica");
        }
    }

    private void validateDepartmentPermissions(SystemUser requester, List<SystemModule> modules) {
        boolean allAllowed = modules.stream()
                .allMatch(module -> module.getAllowedDepartments().contains(requester.getDepartment()));
        if (!allAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departamento sem permissão para acessar este módulo");
        }
    }

    private void validateNoDuplicateRequests(Long userId, List<SystemModule> modules) {
        boolean hasActiveRequests = modules.stream()
                .anyMatch(module -> accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(userId, AccessRequestStatus.ATIVO, module.getId()));
        if (hasActiveRequests) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe solicitação ativa para um dos módulos");
        }
    }

    private void validateNoActiveAccess(Long userId, List<SystemModule> modules) {
        boolean hasAccess = modules.stream()
                .anyMatch(module -> moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(userId, module.getId(), ModuleAccessStatus.ATIVO));
        if (hasAccess) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Módulo já está ativo em seu perfil");
        }
    }

    private void validateMutualExclusion(Long userId, List<SystemModule> modules) {
        Set<String> activeCodes = moduleAccessRepository.findByUserIdAndStatus(userId, ModuleAccessStatus.ATIVO).stream()
                .map(access -> access.getModule().getCode())
                .collect(Collectors.toSet());
        Set<String> requestedCodes = modules.stream().map(SystemModule::getCode).collect(Collectors.toSet());
        for (SystemModule module : modules) {
            boolean conflictWithActive = module.getIncompatibleModules().stream()
                    .map(SystemModule::getCode)
                    .anyMatch(activeCodes::contains);
            boolean conflictWithinRequest = module.getIncompatibleModules().stream()
                    .map(SystemModule::getCode)
                    .anyMatch(requestedCodes::contains);
            if (conflictWithActive || conflictWithinRequest) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Módulo incompatível com outro módulo já ativo em seu perfil");
            }
        }
    }

    private void validateAccessLimit(Long userId, SystemUser requester, int requestedModules) {
        long activeCount = moduleAccessRepository.countByUserIdAndStatus(userId, ModuleAccessStatus.ATIVO);
        int limit = requester.getDepartment() == com.supera.desafio.core.domain.user.DepartmentType.TI ? 10 : 5;
        if (activeCount + requestedModules > limit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite de módulos ativos atingido");
        }
    }

    private String generateProtocol() {
        String date = LocalDate.now(ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        int sequence = accessRequestRepository.findDailySequence(date) + 1;
        return "SOL-" + date + "-" + String.format("%04d", sequence);
    }

    private void addHistory(AccessRequest request, AccessRequestHistoryEventType eventType, String description) {
        AccessRequestHistory history = new AccessRequestHistory();
        history.setEventType(eventType);
        history.setDescription(description);
        history.setRequest(request);
        request.getHistoryEntries().add(history);
    }

    private AccessRequestResponse toResponse(AccessRequest request) {
        List<ModuleDto> modules = request.getModules().stream()
                .map(module -> new ModuleDto(module.getId(), module.getCode(), module.getName(), module.getDescription(), module.isActive()))
                .toList();
        List<AccessRequestHistoryDto> history = request.getHistoryEntries().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(entry -> new AccessRequestHistoryDto(entry.getCreatedAt(), entry.getEventType(), entry.getDescription()))
                .toList();
        return new AccessRequestResponse(
                request.getId(),
                request.getProtocol(),
                request.getStatus(),
                request.isUrgent(),
                request.getRequestedAt(),
                request.getExpiresAt(),
                request.getJustification(),
                request.getDeniedReason(),
                modules,
                history);
    }
}
