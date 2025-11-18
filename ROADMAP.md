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

## Status Atual
- Dia 1 concluído (fundamentos criados e seeds configurados).
- Dia 2 concluído (autenticação e segurança básica entregue).
- Dia 3 concluído (Dockerfile + docker-compose + Nginx balanceando).
