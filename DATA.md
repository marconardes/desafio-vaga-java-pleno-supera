Beleza, é um teste bem completo 😄
Vou montar pra você um **plano de ataque + arquitetura** já pensando em como implementar e o que você pode explicar na entrevista.

Se depois quiser, a gente aprofunda em código (entities, services, testes, Dockerfile, nginx.conf etc.).

---

## 1. Visão Geral da Solução

**Stack (obrigatória):**

* Java 21
* Spring Boot 3.x
* Spring Web / Spring MVC
* Spring Data JPA
* Spring Validation
* Spring Security + JWT
* PostgreSQL 17 (produção/dev via Docker)
* H2 (testes)
* JUnit 5, Mockito, Spring Security Test, MockMvc, JaCoCo, Instancio
* Docker, Docker Compose
* Nginx como Load Balancer / Proxy
* Opcional: Flyway/Liquibase (diferencial)

---

## 2. Modelagem de Domínio

### 2.1. Entidades principais

**User**

* `id`
* `name`
* `email` (único)
* `passwordHash`
* `department` (enum: TI, FINANCEIRO, RH, OPERACOES, OUTRO)
* `enabled`
* `createdAt`

**Module**

* `id`
* `name` (ex: "Portal do Colaborador")
* `code` (ex: `PORTAL`, `RELATORIOS`, `GESTAO_FINANCEIRA`)
* `description`
* `allowedDepartments` (ManyToMany ou tabela join ModuleDepartment)
* `active` (boolean)
* `incompatibleModules` (ManyToMany para Module, auto-relacionamento)

**UserModuleAccess**

* Representa o acesso **ativo** do usuário a um módulo
* `id`
* `user` (ManyToOne)
* `module` (ManyToOne)
* `grantedAt`
* `expiresAt`
* `active` (boolean)

**AccessRequest** (Solicitação)

* `id`
* `protocol` (SOL-YYYYMMDD-NNNN)
* `requester` (User)
* `departmentSnapshot` (String ou enum – snapshot do depto no momento)
* `modules` (OneToMany → AccessRequestModule)
* `justification`
* `urgent` (boolean)
* `status` (enum: ATIVO, NEGADO, CANCELADO)
* `createdAt`
* `updatedAt`
* `previousRequest` (self reference, para renovação)
* `denialReason` (String, nullable)
* `expiresAt` (se aprovado: +180 dias)

**AccessRequestModule**

* `id`
* `accessRequest` (ManyToOne)
* `module` (ManyToOne)

**AccessRequestHistory**

* Para histórico de alterações
* `id`
* `accessRequest`
* `eventType` (enum: CREATED, APPROVED, DENIED, RENEWED, CANCELLED)
* `description` (ex: "Solicitação negada: Departamento sem permissão...")
* `createdAt`

---

## 3. Regras de Negócio – Onde colocar?

Criar um **service especializado** para as regras:

* `AccessRequestService`
* `AccessValidationService` (ou `AccessRulesEngine`)

O fluxo do `createAccessRequest()`:

1. Validar entrada (DTO + Bean Validation):

   * 1–3 módulos
   * justificação 20–500 chars
   * texto não genérico (regras simples: tamanho + blacklist: “teste”, “aaa”, “preciso” etc).
2. Buscar usuário logado via `SecurityContext`.
3. Validar:

   * Não ter `AccessRequest` ATIVA para o mesmo módulo.
   * Não ter `UserModuleAccess` ativo para o módulo.
   * Módulo ativo e disponível.
4. Validar compatibilidade de departamento:

   * Mapear `department → módulos permitidos`.
5. Verificar exclusões mútuas:

   * `APROVADOR_FINANCEIRO` vs `SOLICITANTE_FINANCEIRO`
   * `ADMINISTRADOR_RH` vs `COLABORADOR_RH`
   * Checar tanto módulos já ativos quanto os solicitados juntos.
6. Verificar limite de módulos:

   * Padrão: máx. 5
   * TI: máx. 10
7. Se qualquer regra falhar:

   * `status = NEGADO`
   * `denialReason` com um dos textos:

     * "Departamento sem permissão para acessar este módulo"
     * "Módulo incompatível com outro módulo já ativo em seu perfil"
     * "Limite de módulos ativos atingido"
     * "Justificativa insuficiente ou genérica"
8. Se todas passarem:

   * Criar `UserModuleAccess` para cada módulo.
   * `status = ATIVO`
   * `expiresAt = now() + 180 dias`
9. Gerar `protocol` no formato `SOL-YYYYMMDD-NNNN`:

   * `YYYYMMDD` da data atual
   * `NNNN` sequência do dia (pode usar sequence no banco ou contar requests do dia).
10. Registrar `AccessRequestHistory`.

**Renovação:**

* Apenas se:

  * Solicitação original pertence ao usuário logado
  * Status ATIVO
  * `expiresAt` faltando < 30 dias
* Criar **nova** `AccessRequest`:

  * `previousRequest` apontando para a original
  * Reaplicar todas as regras atuais
  * Se aprovada: novos `UserModuleAccess` (ou atualizar expirations dos existentes) + `expiresAt = +180 dias`.

**Cancelamento:**

* Só pode cancelar se:

  * Usuário é o dono
  * Status atual = ATIVO
* Ao cancelar:

  * Validar justificativa 10–200 chars
  * `status = CANCELADO`
  * Desativar `UserModuleAccess` correspondentes
  * Registrar no histórico.

---

## 4. Autenticação, Autorização e Segurança

### 4.1. Spring Security + JWT

* Endpoint `POST /auth/login`:

  * Recebe `{ email, password }`
  * Valida credenciais (buscar User por email, comparar hash com BCrypt).
  * Gera JWT com:

    * `sub` = userId
    * `email`
    * `department`
    * `exp` = agora + 15 minutos
* Opcional (diferencial): `POST /auth/refresh` com refresh token.

### 4.2. Configuração

* Filtro JWT:

  * Lê Authorization: Bearer <token>
  * Valida assinatura, expiração
  * Cria `UsernamePasswordAuthenticationToken` com o userId e roles (se precisar).
* SecurityConfig:

  * `/auth/**` → `permitAll()`
  * `/swagger-ui/**`, `/v3/api-docs/**` → `permitAll()` (ou protegido, a critério, mas documente)
  * Demais endpoints → `authenticated()`
* Garantir que **consultas/alterações** só enxerguem dados do usuário logado:

  * Ex.: no repository: `findByIdAndRequesterId(...)`
  * Ou checar no service: se `request.getRequester().getId() != currentUserId` → 403.

### 4.3. Senhas

* Usar `BCryptPasswordEncoder`.
* No `data.sql` ou migrations, já inserir senhas criptografadas.

---

## 5. Endpoints Principais (REST)

Padrão `/api` ou `/` direto, documentado no Swagger:

### 5.1. Auth

* `POST /auth/login`
* (Opcional) `POST /auth/refresh`

### 5.2. Módulos

* `GET /modules`

  * Lista todos módulos disponíveis com:

    * nome
    * descrição
    * departamentos permitidos
    * ativo
    * módulos incompatíveis

### 5.3. Solicitações de Acesso

* `POST /access-requests`

  * Body: módulos (ids ou codes), justificativa, urgente
  * Retorno: protocolo, status, mensagem amigável de sucesso/negação.

* `GET /access-requests`

  * Filtros:

    * `q` (texto: protocolo ou nome de módulo)
    * `status`
    * `startDate`, `endDate`
    * `urgent`
    * `page`, `size` (size fixo em 10 no backend ou configurável, mas retornar 10 por página)
  * Sempre filtrando pelo `userId` do token.
  * Ordenação default: `createdAt DESC`.

* `GET /access-requests/{id}`

  * Detalhes completos
  * Verifica se pertence ao usuário

* `POST /access-requests/{id}/renew`

  * Faz as regras de renovação

* `POST /access-requests/{id}/cancel`

  * Body: `{ reason }`
  * Cancela + revoga acessos

---

## 6. Testes (estratégia para bater 80%+ JaCoCo)

### 6.1. Ferramentas

* JUnit 5
* Mockito
* Spring Boot Test
* MockMvc
* Spring Security Test
* Instancio (para gerar objetos complexos)

### 6.2. Regras importantes

* **Não usar** `any()`, `anyString()` etc
* Sempre `eq()` ou valores específicos:

  * `when(repo.save(eq(entity)))`
  * `verify(repo).findByUserIdAndStatus(eq(userId), eq(Status.ATIVO))`
* Cobrir:

  * Todos os métodos de Service
  * Todos os cenários de regra de negócio:

    * Departamento sem permissão
    * Incompatibilidade de módulos
    * Limite estourado
    * Justificativa genérica
    * Já tem solicitação ativa
    * Já tem acesso ativo
  * Exceções: 400, 403, 404, etc.

### 6.3. Tipos de testes

* **Unitários (Service / RulesEngine)**:

  * Mock de repositories
  * Sem contexto web.
* **Integração (Controller + Security + DB H2)**:

  * `@SpringBootTest` + `@AutoConfigureMockMvc`
  * Testar login, criação de solicitação, filtros, autorização.
* **JaCoCo**:

  * Configurar `jacoco-maven-plugin` para falhar build se `< 80%`.

---

## 7. Docker, Docker Compose e Nginx

### 7.1. Dockerfile (multi-stage)

* Stage 1: build

  * `maven:3.9-eclipse-temurin-21` (por exemplo)
  * `mvn clean package -DskipTests=false`
* Stage 2: runtime

  * `eclipse-temurin:21-jre`
  * Copiar `.jar`
  * `ENTRYPOINT ["java","-jar","/app/app.jar"]`
* Usar variáveis de ambiente para:

  * `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  * `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`

### 7.2. docker-compose.yml

Serviços:

* `db` (postgres:17):

  * `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
  * Volume para dados, porta interna 5432
* `app1`, `app2`, `app3`:

  * Build a partir do Dockerfile
  * Depende de `db`
  * Mesma imagem, só muda container_name
  * `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/...`
* `nginx`:

  * `depends_on: [app1, app2, app3]`
  * Volume com `nginx.conf`
  * Expor porta 80
* Rede:

  * `network: app-network`
  * Todos os serviços na mesma network.

### 7.3. Nginx (balanceamento simples round-robin)

`upstream app_backend {
server app1:8080;
server app2:8080;
server app3:8080;
}

server {
listen 80;
location / {
proxy_pass http://app_backend;
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
}
}`

* Expor `/swagger-ui.html` pelo mesmo proxy (não precisa config especial, ele vai pela rota padrão).

---

## 8. README.md (estrutura recomendada)

* Descrição do projeto
* Tecnologias e versões
* Pré-requisitos (Docker, Docker Compose, Java 21 se quiser rodar sem Docker)
* Como subir com Docker:

  * `mvn clean package`
  * `docker-compose up --build`
* Como rodar testes:

  * `mvn test`
  * `mvn verify` (para JaCoCo)
* Como ver relatório de cobertura:

  * `target/site/jacoco/index.html`
* Credenciais de teste:

  * Ex.: `ti.user@corp.com / 123456`
* Endpoints principais + exemplos (curl ou JSON do Postman)
* Arquitetura da solução:

  * Diagrama curto (texto ou imagem) explicando:

    * API → DB
    * Três instâncias → Nginx
    * JWT auth
* Decisões técnicas:

  * Por que usou restrições X, enums, etc.
  * Suas suposições (ex.: regra de renovação atualiza ou cria novo access).

---

## 9. Roadmap de Implementação (pra você se organizar nos 8 dias)

1. **Dia 1**

   * Criar projeto Spring Boot (Java 21, dependências)
   * Configurar Postgres local e Docker Compose básico
   * Modelar entidades + repositories
   * Configurar Flyway/Liquibase (se for usar)
   * Popular dados iniciais (usuários + módulos)

2. **Dia 2**

   * Implementar autenticação JWT + login
   * Criar endpoints de módulos (`GET /modules`)
   * Implementar criação de solicitação + regras de negócio

3. **Dia 3**

   * Ajustar cobertura JaCoCo
   * Finalizar Dockerfile multi-stage + compose com 3 apps + Nginx
   * Validar `docker-compose up` do zero


4. **Dia 4**

   * Consultas / filtros / paginação
   * Detalhes / renovação / cancelamento

4. **Dia 5**

   * Testes unitários (services / regras)
   * Testes de integração (MockMvc)


6. **Dia 6**

   * Polir README
   * Gerar relatório JaCoCo
   * Revisar tudo como se fosse um avaliador (clonar em pasta nova e rodar do zero).

---
