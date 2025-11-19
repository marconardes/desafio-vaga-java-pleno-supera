package com.supera.desafio.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.supera.desafio.access.dto.AccessRequestCancelRequest;
import com.supera.desafio.access.dto.AccessRequestCreateRequest;
import com.supera.desafio.core.domain.module.ModuleAccess;
import com.supera.desafio.core.domain.module.ModuleAccessStatus;
import com.supera.desafio.core.domain.module.SystemModule;
import com.supera.desafio.core.domain.request.AccessRequest;
import com.supera.desafio.core.domain.request.AccessRequestStatus;
import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.core.domain.user.SystemUser;
import com.supera.desafio.core.repository.AccessRequestRepository;
import com.supera.desafio.core.repository.ModuleAccessRepository;
import com.supera.desafio.core.repository.SystemModuleRepository;
import com.supera.desafio.core.repository.SystemUserRepository;
import com.supera.desafio.security.model.AuthenticatedUser;
import com.supera.desafio.security.service.CurrentUserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AccessRequestServiceTest {

    private AccessRequestRepository accessRequestRepository;
    private SystemModuleRepository moduleRepository;
    private ModuleAccessRepository moduleAccessRepository;
    private SystemUserRepository userRepository;
    private CurrentUserService currentUserService;
    private AccessRequestService service;

    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "carla.ti@corp.com", "Carla", DepartmentType.TI);

    @BeforeEach
    void setup() {
        accessRequestRepository = mock(AccessRequestRepository.class, invocation -> {
            if ("save".equals(invocation.getMethod().getName())) {
                return invocation.getArgument(0);
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
        moduleRepository = mock(SystemModuleRepository.class);
        moduleAccessRepository = mock(ModuleAccessRepository.class);
        userRepository = mock(SystemUserRepository.class);
        currentUserService = mock(CurrentUserService.class);
        service = new AccessRequestService(accessRequestRepository, moduleRepository, moduleAccessRepository, userRepository, currentUserService);
        when(currentUserService.requireUser()).thenReturn(authenticatedUser);
    }

    @Test
    void shouldCreateRequestAndGrantAccess() {
        SystemUser user = buildUser();
        SystemModule module = buildModule(10L, "PORTAL", DepartmentType.TI);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(10L)))).thenReturn(List.of(module));
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(10L))).thenReturn(false);
        when(moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(eq(1L), eq(10L), eq(ModuleAccessStatus.ATIVO))).thenReturn(false);
        when(moduleAccessRepository.countByUserIdAndStatus(eq(1L), eq(ModuleAccessStatus.ATIVO))).thenReturn(0L);
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        when(accessRequestRepository.findDailySequence(eq(today))).thenReturn(0);

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(10L), "Necessito acessar o portal para rotinas.", false);
        var response = service.create(payload);

        assertEquals(AccessRequestStatus.ATIVO, response.status());
        assertEquals(AccessRequestStatus.ATIVO, response.status());
        ArgumentCaptor<ModuleAccess> moduleCaptor = ArgumentCaptor.forClass(ModuleAccess.class);
        verify(moduleAccessRepository, times(1)).save(moduleCaptor.capture());
        assertEquals(ModuleAccessStatus.ATIVO, moduleCaptor.getValue().getStatus());
    }

    @Test
    void shouldFailWhenDepartmentNotAllowed() {
        SystemUser user = buildUser();
        SystemModule module = buildModule(11L, "FIN", DepartmentType.FINANCEIRO);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(11L)))).thenReturn(List.of(module));

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(11L), "Justificativa suficiente para módulo.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailWhenLimitExceeded() {
        SystemUser user = buildUser();
        SystemModule module = buildModule(12L, "REL", DepartmentType.TI);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(12L)))).thenReturn(List.of(module));
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(12L))).thenReturn(false);
        when(moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(eq(1L), eq(12L), eq(ModuleAccessStatus.ATIVO))).thenReturn(false);
        when(moduleAccessRepository.countByUserIdAndStatus(eq(1L), eq(ModuleAccessStatus.ATIVO))).thenReturn(10L);

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(12L), "Justificativa válida para testes.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldEnforceLimitForNonTi() {
        SystemUser user = buildUser();
        user.setDepartment(DepartmentType.FINANCEIRO);
        SystemModule module = buildModule(99L, "FIN", DepartmentType.FINANCEIRO);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(99L)))).thenReturn(List.of(module));
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(99L))).thenReturn(false);
        when(moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(eq(1L), eq(99L), eq(ModuleAccessStatus.ATIVO))).thenReturn(false);
        when(moduleAccessRepository.countByUserIdAndStatus(eq(1L), eq(ModuleAccessStatus.ATIVO))).thenReturn(5L);

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(99L), "Justificativa válida para limites.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailWhenJustificationIsGeneric() {
        SystemUser user = buildUser();
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(99L), "teste", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(moduleRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldFailWhenModuleListMismatch() {
        SystemUser user = buildUser();
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(50L)))).thenReturn(List.of());
        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(50L), "Justificativa válida para mismatch.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailWhenModuleInactive() {
        SystemUser user = buildUser();
        SystemModule module = buildModule(51L, "PORTAL", DepartmentType.TI);
        module.setActive(false);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(51L)))).thenReturn(List.of(module));
        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(51L), "Justificativa válida para inativo.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailWhenDuplicateRequestExists() {
        SystemUser user = buildUser();
        SystemModule module = buildModule(12L, "REL", DepartmentType.TI);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(12L)))).thenReturn(List.of(module));
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(12L))).thenReturn(true);

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(12L), "Justificativa válida para testes.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailWhenModuleAlreadyActive() {
        SystemUser user = buildUser();
        SystemModule module = buildModule(13L, "REL", DepartmentType.TI);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(13L)))).thenReturn(List.of(module));
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(13L))).thenReturn(false);
        when(moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(eq(1L), eq(13L), eq(ModuleAccessStatus.ATIVO))).thenReturn(true);

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(13L), "Justificativa válida para testes.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailWhenModulesAreIncompatible() {
        SystemUser user = buildUser();
        SystemModule moduleA = buildModule(14L, "APROVADOR_FIN", DepartmentType.TI);
        SystemModule moduleB = buildModule(15L, "SOLICITANTE_FIN", DepartmentType.TI);
        moduleA.getIncompatibleModules().add(moduleB);
        moduleB.getIncompatibleModules().add(moduleA);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(14L, 15L)))).thenReturn(List.of(moduleA, moduleB));
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(14L))).thenReturn(false);
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(15L))).thenReturn(false);
        when(moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(eq(1L), eq(14L), eq(ModuleAccessStatus.ATIVO))).thenReturn(false);
        when(moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(eq(1L), eq(15L), eq(ModuleAccessStatus.ATIVO))).thenReturn(false);
        when(moduleAccessRepository.countByUserIdAndStatus(eq(1L), eq(ModuleAccessStatus.ATIVO))).thenReturn(0L);
        when(moduleAccessRepository.findByUserIdAndStatus(eq(1L), eq(ModuleAccessStatus.ATIVO))).thenReturn(List.of());

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(14L, 15L), "Justificativa válida para incompatibilidade.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailWhenActiveAccessIncompatible() {
        SystemUser user = buildUser();
        SystemModule module = buildModule(16L, "APROVADOR_FIN", DepartmentType.TI);
        SystemModule incompatible = buildModule(17L, "SOLICITANTE_FIN", DepartmentType.TI);
        module.getIncompatibleModules().add(incompatible);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(moduleRepository.findAllById(eq(List.of(16L)))).thenReturn(List.of(module));
        when(accessRequestRepository.existsByRequesterIdAndStatusAndModules_Id(eq(1L), eq(AccessRequestStatus.ATIVO), eq(16L))).thenReturn(false);
        when(moduleAccessRepository.existsByUserIdAndModuleIdAndStatus(eq(1L), eq(16L), eq(ModuleAccessStatus.ATIVO))).thenReturn(false);
        when(moduleAccessRepository.countByUserIdAndStatus(eq(1L), eq(ModuleAccessStatus.ATIVO))).thenReturn(0L);
        ModuleAccess active = new ModuleAccess();
        active.setModule(incompatible);
        when(moduleAccessRepository.findByUserIdAndStatus(eq(1L), eq(ModuleAccessStatus.ATIVO))).thenReturn(List.of(active));

        AccessRequestCreateRequest payload = new AccessRequestCreateRequest(List.of(16L), "Acesso necessita compatibilidade.", false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldCancelRequestAndRevokeAccess() {
        SystemUser user = buildUser();
        AccessRequest saved = new AccessRequest();
        saved.setId(20L);
        saved.setStatus(AccessRequestStatus.ATIVO);
        saved.setRequester(user);
        saved.setRequesterDepartment(user.getDepartment());
        SystemModule module = buildModule(15L, "PORTAL", DepartmentType.TI);
        saved.getModules().add(module);

        when(accessRequestRepository.findByIdAndRequesterId(eq(20L), eq(1L))).thenReturn(Optional.of(saved));
        ModuleAccess moduleAccess = new ModuleAccess();
        moduleAccess.setId(30L);
        moduleAccess.setModule(module);
        moduleAccess.setUser(user);
        moduleAccess.setStatus(ModuleAccessStatus.ATIVO);
        when(moduleAccessRepository.findByUserIdAndModuleIdAndStatus(eq(1L), eq(15L), eq(ModuleAccessStatus.ATIVO))).thenReturn(Optional.of(moduleAccess));

        AccessRequestCancelRequest payload = new AccessRequestCancelRequest("Motivo válido para cancelamento");
        var response = service.cancel(20L, payload);
        assertEquals(AccessRequestStatus.CANCELADO, response.status());
        ArgumentCaptor<ModuleAccess> captor = ArgumentCaptor.forClass(ModuleAccess.class);
        verify(moduleAccessRepository).save(captor.capture());
        assertEquals(ModuleAccessStatus.REVOGADO, captor.getValue().getStatus());
    }

    @Test
    void shouldFailCancelWhenRequestNotActive() {
        SystemUser user = buildUser();
        AccessRequest saved = new AccessRequest();
        saved.setId(22L);
        saved.setStatus(AccessRequestStatus.NEGADO);
        saved.setRequester(user);
        when(accessRequestRepository.findByIdAndRequesterId(eq(22L), eq(1L))).thenReturn(Optional.of(saved));

        AccessRequestCancelRequest payload = new AccessRequestCancelRequest("Motivo válido para cancelamento");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.cancel(22L, payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private SystemUser buildUser() {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setDepartment(DepartmentType.TI);
        user.setFullName("Carla Tech");
        user.setEmail("carla.ti@corp.com");
        user.setPassword("hash");
        return user;
    }

    private SystemModule buildModule(Long id, String code, DepartmentType allowed) {
        SystemModule module = new SystemModule();
        module.setId(id);
        module.setCode(code);
        module.setName(code + " Module");
        module.setDescription("desc");
        module.getAllowedDepartments().add(allowed);
        module.setActive(true);
        return module;
    }

    @Test
    void shouldRenewRequestWhenCloseToExpire() {
        SystemUser user = buildUser();
        AccessRequest original = new AccessRequest();
        original.setId(30L);
        original.setStatus(AccessRequestStatus.ATIVO);
        original.setRequester(user);
        original.setRequesterDepartment(user.getDepartment());
        original.setProtocol("SOL-20250101-0001");
        original.setRequestedAt(Instant.now().minus(160, ChronoUnit.DAYS));
        original.setExpiresAt(Instant.now().plus(10, ChronoUnit.DAYS));
        SystemModule module = buildModule(18L, "PORTAL", DepartmentType.TI);
        original.getModules().add(module);

        when(accessRequestRepository.findByIdAndRequesterId(eq(30L), eq(1L))).thenReturn(Optional.of(original));
        when(accessRequestRepository.findDailySequence(org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        ModuleAccess moduleAccess = new ModuleAccess();
        moduleAccess.setModule(module);
        moduleAccess.setUser(user);
        moduleAccess.setStatus(ModuleAccessStatus.ATIVO);
        when(moduleAccessRepository.findByUserIdAndModuleIdAndStatus(eq(1L), eq(18L), eq(ModuleAccessStatus.ATIVO))).thenReturn(Optional.of(moduleAccess));

        var response = service.renew(30L);
        assertEquals(AccessRequestStatus.ATIVO, response.status());
        verify(accessRequestRepository, times(1)).save(org.mockito.ArgumentMatchers.any(AccessRequest.class));
    }

    @Test
    void shouldFailRenewWhenNotCloseToExpire() {
        SystemUser user = buildUser();
        AccessRequest original = new AccessRequest();
        original.setId(31L);
        original.setStatus(AccessRequestStatus.ATIVO);
        original.setRequester(user);
        original.setExpiresAt(Instant.now().plus(60, ChronoUnit.DAYS));
        when(accessRequestRepository.findByIdAndRequesterId(eq(31L), eq(1L))).thenReturn(Optional.of(original));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.renew(31L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailRenewWhenRequestNotActive() {
        SystemUser user = buildUser();
        AccessRequest original = new AccessRequest();
        original.setId(32L);
        original.setStatus(AccessRequestStatus.NEGADO);
        original.setRequester(user);
        when(accessRequestRepository.findByIdAndRequesterId(eq(32L), eq(1L))).thenReturn(Optional.of(original));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.renew(32L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldFailRenewWhenExpirationNull() {
        SystemUser user = buildUser();
        AccessRequest original = new AccessRequest();
        original.setId(33L);
        original.setStatus(AccessRequestStatus.ATIVO);
        original.setRequester(user);
        original.setExpiresAt(null);
        when(accessRequestRepository.findByIdAndRequesterId(eq(33L), eq(1L))).thenReturn(Optional.of(original));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.renew(33L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldCancelEvenWithoutActiveAccess() {
        SystemUser user = buildUser();
        AccessRequest saved = new AccessRequest();
        saved.setId(90L);
        saved.setStatus(AccessRequestStatus.ATIVO);
        saved.setRequester(user);
        SystemModule module = buildModule(91L, "PORTAL", DepartmentType.TI);
        saved.getModules().add(module);
        when(accessRequestRepository.findByIdAndRequesterId(eq(90L), eq(1L))).thenReturn(Optional.of(saved));
        when(moduleAccessRepository.findByUserIdAndModuleIdAndStatus(eq(1L), eq(91L), eq(ModuleAccessStatus.ATIVO))).thenReturn(Optional.empty());

        AccessRequestCancelRequest payload = new AccessRequestCancelRequest("Motivo válido");
        var response = service.cancel(90L, payload);
        assertEquals(AccessRequestStatus.CANCELADO, response.status());
    }

    @Test
    void shouldRenewEvenWithoutExistingAccessRecord() {
        SystemUser user = buildUser();
        AccessRequest original = new AccessRequest();
        original.setId(92L);
        original.setStatus(AccessRequestStatus.ATIVO);
        original.setRequester(user);
        original.setRequesterDepartment(user.getDepartment());
        original.setProtocol("SOL-OLD");
        original.setRequestedAt(Instant.now().minus(150, ChronoUnit.DAYS));
        original.setExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));
        SystemModule module = buildModule(93L, "PORTAL", DepartmentType.TI);
        original.getModules().add(module);

        when(accessRequestRepository.findByIdAndRequesterId(eq(92L), eq(1L))).thenReturn(Optional.of(original));
        when(accessRequestRepository.findDailySequence(org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        when(moduleAccessRepository.findByUserIdAndModuleIdAndStatus(eq(1L), eq(93L), eq(ModuleAccessStatus.ATIVO))).thenReturn(Optional.empty());

        var response = service.renew(92L);
        assertEquals(AccessRequestStatus.ATIVO, response.status());
    }

    @Test
    void shouldListRequestsWithPagination() {
        SystemUser user = buildUser();
        AccessRequest request = new AccessRequest();
        request.setId(40L);
        request.setRequester(user);
        request.setStatus(AccessRequestStatus.ATIVO);
        request.setProtocol("SOL-20250101-0005");
        Page<AccessRequest> page = new PageImpl<>(List.of(request), PageRequest.of(0, 10), 1);
        when(accessRequestRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<AccessRequest>>any(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var response = service.list("SOL", AccessRequestStatus.ATIVO, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(), true, 0, 10);
        assertEquals(1, response.totalElements());
        assertEquals(40L, response.content().get(0).id());
    }

    @Test
    void shouldListRequestsWithoutFilters() {
        Page<AccessRequest> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(accessRequestRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<AccessRequest>>any(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var response = service.list(null, null, null, null, null, 0, null);
        assertEquals(0, response.totalElements());
    }
}
