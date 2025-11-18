# Roadmap de Implementação

## Macro Fases
1. **Fundamentos**
   - Ajustar `pom.xml` com todas as dependências (Security, JWT, Flyway, Springdoc, testes) e plugins (JaCoCo, Surefire, Compiler).
   - Modelar domínio completo (usuários, módulos, acessos, solicitações, histórico, enums).
   - Configurar Flyway + dados iniciais (usuários e módulos exigidos).
2. **Segurança e Autenticação**
   - Implementar login JWT, criptografia de senha, refresh opcional e filtros de segurança.
   - Restringir endpoints e garantir acesso somente aos dados do usuário autenticado.
3. **Solicitações e Regras de Negócio**
   - Implementar criação, listagem, detalhamento, renovação e cancelamento de solicitações, com todas as validações.
4. **Testes e Qualidade**
   - Cobrir services com testes unitários (Mockito sem `any()`), testes de integração com MockMvc/Security e configurar JaCoCo (mínimo 90%).
5. **Documentação e Observabilidade**
   - Swagger/OpenAPI, README completo, relatório JaCoCo em PDF, diagramas/ADRs (opcional).
6. **Infraestrutura Docker**
   - Dockerfile multi-stage, docker-compose com Postgres + 3 instâncias da API + Nginx balanceando e expondo Swagger.
7. **Finalização**
   - Testes end-to-end, checklist final, revisão do repositório e empacotamento para entrega.

## Cronograma por Dia
- **Dia 1**: Fundamentos — configurar projeto Maven/Java 21 com todas as dependências, preparar estrutura de pacotes, modelar entidades iniciais e criar migrations Flyway com seeds (usuários + módulos + incompatibilidades) já alinhados aos requisitos descritos no `DATA.md`.
- **Dia 2**: Autenticação básica — Spring Security com JWT (15 min), hash BCrypt, endpoints `/auth/login`, filtro global e testes.
- **Dia 3**: CRUD de solicitações e motor de regras (criação + validações principais).
- **Dia 4**: Consulta, detalhamento, renovação e cancelamento de solicitações, além de consulta de módulos.
- **Dia 5**: Ampliar testes unitários/integração e garantir cobertura >90% pelo JaCoCo.
- **Dia 6**: Dockerfile multi-stage, docker-compose com 3 apps + Nginx, healthchecks.
- **Dia 7**: Swagger, documentação completa, exemplos de requisições, decisões técnicas.
- **Dia 8**: Rodadas finais (mvn verify, docker compose up), gerar relatório JaCoCo em PDF, revisar e preparar entrega.

## Status Atual
- Dia 1 concluído (fundamentos criados e seeds configurados).
- Dia 2 concluído (autenticação e segurança básica entregue).
