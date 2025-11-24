# Desafio Java Pleno Supera

API e infraestrutura de suporte para o fluxo de Solicitações de Acesso a Módulos. Toda a documentação técnica e o plano detalhado de execução estão em `ROADMAP.md`, enquanto `DATA.md` descreve hipóteses de regras e o racional de negócio.

## Visão Geral
- Catálogo de módulos corporativos, solicitações com limite e compatibilidade entre perfis, cancelamento e renovação em fila única.
- Autenticação JWT com expiração curta e validação stateless.
- Stack containerizada com Postgres 17, três instâncias da API e Nginx balanceando as requisições.
- Migrações Flyway e seeds garantem setup consistente para reviewers.

## Tecnologias
- Java 21, Spring Boot 3.2, Spring Web, Spring Data JPA, Spring Validation.
- Spring Security + JWT (jjwt), Bean Validation e GlobalExceptionHandler.
- PostgreSQL 17 (prod/dev) e H2 para testes.
- Flyway, Docker/Docker Compose, Nginx como reverse proxy.
- JUnit 5, Mockito, Spring Security Test, MockMvc, Instancio, JaCoCo (mínimo 90% instruction coverage).
- Vue 3, Pinia e Vite para a SPA corporativa hospedada em `frontend/`.

## Arquitetura
```
┌────────┐     ┌────────┐     ┌───────────────┐
│ Client │ ──▶ │ Nginx  │ ──▶ │ app1/app2/app3│
└────────┘     └────────┘     └───────────────┘
                                 │
                                 ▼
                            PostgreSQL 17
```
- O `docker-compose.yml` sobe `db`, três containers idênticos da API (build multi-stage) e um Nginx (`nginx/default.conf`) que faz round-robin apontando para as portas 8080 internas.
- `SPRING_PROFILES_ACTIVE=prod` habilita configuração otimizada para containers e leitura de variáveis externas.
- `application.yml` centraliza os defaults e expõe `actuator/health` para os healthchecks do Dockerfile e do Compose.

## Frontend
- Nesta versão a entrega contempla apenas a API. O diretório `frontend/` permanece com o código Vue 3 para referência, porém não é executado automaticamente pelo docker-compose.

## Pré-requisitos
- Docker 24+ e Docker Compose plugin.
- Make sure porta `8080` está livre (exposta pelo Nginx).
- Para rodar localmente sem Docker: Java 21+, Maven 3.9+, Postgres 17 (ou Compose apenas para o banco).

## Como executar

### Ambiente completo (Docker Compose)
```bash
# (Opcional) preparar dependências em cache
mvn clean package

# Build da imagem e subida dos serviços
docker compose up --build
```
- Espere o healthcheck do Postgres completar; Nginx só entra após as três APIs estarem saudáveis.
- Acesse `http://localhost:8080` (API) ou `http://localhost:8080/swagger-ui/index.html` para a documentação interativa.
- Para resetar o ambiente: `docker compose down -v`.

### Execução local sem containers
1. Suba apenas o Postgres (local ou via `docker compose up db -d`).
2. Exporte as variáveis esperadas ou ajuste `application.yml`:
   ```bash
   export DB_HOST=localhost
   export DB_PORT=5432
   export DB_NAME=desafio
   export DB_USER=supera
   export DB_PASSWORD=supera
   export JWT_SECRET=local-dev-secret
   ```
3. Rode a aplicação:
   ```bash
   mvn spring-boot:run
   ```
4. Por padrão a API responde em `http://localhost:8080`.

## Variáveis de ambiente principais
| Variável | Descrição | Default |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` | Host/porta do Postgres | `db` / `5432` |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Credenciais do schema | `desafio`, `supera`, `supera` |
| `JWT_SECRET` | Segredo para assinatura dos tokens | `this-is-a-dev-secret-change-me` (compose usa `change-me-secret`) |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring | `prod` nos containers / `default` local |
| `SERVER_PORT` | Porta da API | `8080` |
| `TZ`, `JAVA_OPTS` | Ajustes opcionais do container | definidos no `Dockerfile` |

Use um arquivo `.env` na raiz para sobrescrever valores durante o `docker compose up`.

## Usuários seed (todos com `Senha123`)
| Nome | E-mail | Departamento |
| --- | --- | --- |
| Carla Tech | `carla.ti@corp.com` | TI |
| Paulo Financeiro | `paulo.financeiro@corp.com` | FINANCEIRO |
| Renata RH | `renata.rh@corp.com` | RH |
| Otavio Operações | `otavio.operacoes@corp.com` | OPERAÇÕES |

As senhas já estão criptografadas nas migrations (`src/main/resources/db/migration/V2__seed_data.sql`). Utilize esses usuários para autenticar no Swagger ou via frontend (`frontend/dist` contém um build Vue pronto para servir manualmente, fora do escopo do Compose).

## Testes, qualidade e cobertura
```bash
# Unitários + integração
mvn clean test

# Inclui relatório JaCoCo (falha se < 90% instruction coverage)
mvn clean verify
```
- O relatório HTML fica em `target/site/jacoco/index.html`.
- Os testes cobrem services, filtros JWT, controllers (MockMvc) e cenários de regra de negócio (limites, incompatibilidades, departamentos, etc.).

## Endpoints principais
| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/auth/login` | Autentica usuário corporativo e retorna tokens + snapshot do perfil. |
| `GET` | `/api/modules` | Lista módulos disponíveis, descrição, status e restrições de departamento. |
| `POST` | `/api/access-requests` | Cria solicitação (1–3 módulos). Aplica regras de incompatibilidade, limites e justificativa. |
| `GET` | `/api/access-requests` | Consulta paginada com filtros (`q`, `status`, `urgent`, data). Retorna apenas dados do solicitante logado. |
| `GET` | `/api/access-requests/{id}` | Detalhes completos (histórico e módulos). |
| `POST` | `/api/access-requests/{id}/cancel` | Cancela solicitações ativas com registro de histórico e revogação dos acessos. |
| `POST` | `/api/access-requests/{id}/renew` | Renova solicitações que expiram em até 30 dias, reaplicando todas as validações. |
| `GET` | `/actuator/health` | Utilizado por Docker/Nginx para healthcheck. |

Cada endpoint está documentado pelo SpringDoc e exige token JWT (exceto `/auth/**`).

## Estrutura de pastas
- `src/main/java/com/supera/desafio` – Camadas de domínio (`access`, `module`, `security`, `shared`).
- `src/main/resources/db/migration` – Migrações Flyway.
- `nginx/` – Configuração de proxy balanceado.
- `frontend/` – Build da SPA (Vue 3 + Pinia + Vite) usada nos vídeos/demo.

## Próximos passos sugeridos
- Deploy estático do `frontend/dist` no mesmo Nginx (config adicional).
- Adicionar refresh token endpoint e rotação automática.
- Automatizar validações com GitHub Actions (`mvn verify` + `docker compose config`).

Qualquer novidade ou ajuste adicional, registre no `ROADMAP.md`.

## Tutorial rápido (CLI)
1. Clone este repositório e acesse a pasta.
2. Execute `mvn clean package` para validar dependências e buildar a API.
3. Suba a stack: `docker compose up --build` (Postgres + API + Nginx).
4. Faça login via Swagger (`http://localhost:8080/swagger-ui/index.html`) utilizando um dos usuários seed.
5. Opcional: rodar o frontend localmente (fora do Compose) com `cd frontend && npm install && npm run dev -- --host`.
