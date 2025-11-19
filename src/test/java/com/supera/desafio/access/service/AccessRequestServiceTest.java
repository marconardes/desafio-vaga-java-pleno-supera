package com.supera.desafio.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

        assertNotNull(response.protocol());
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
}
