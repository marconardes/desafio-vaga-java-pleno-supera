# Desafio Java Pleno Supera

Projeto inicial para o fluxo de Solicitações de Acesso a Módulos. Consulte `ROADMAP.md` para o plano completo de execução.

## Execução com Docker Compose
1. Gere o artefato localmente (opcional, pois o Dockerfile executa o build):
   ```bash
   mvn clean package
   ```
2. Suba toda a stack (Postgres + 3 instâncias da API + Nginx) com:
   ```bash
   docker compose up --build
   ```
3. Acesse `http://localhost` para chegar na API via Nginx (Swagger ficará exposto em `/swagger-ui.html` quando configurado).

### Variáveis padrão
- `DB_NAME=desafio`
- `DB_USER=supera`
- `DB_PASSWORD=supera`
- `JWT_SECRET=change-me-secret`

Você pode sobrescrever essas variáveis via `.env` ou exportando-as antes do `docker compose up`.
