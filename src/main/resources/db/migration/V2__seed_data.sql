INSERT INTO users (full_name, email, password, department)
VALUES
    ('Carla Tech', 'carla.ti@corp.com', '$2b$12$JeLLjBHolTLiSl3LgAaamOHP9uHcDLBG0gvDDrvZkSzav6Zj2nKGe', 'TI'),
    ('Paulo Financeiro', 'paulo.financeiro@corp.com', '$2b$12$dZLZS16d05V0XyG7Qs2XBe506bmnlWKBCOeUZgwhVOvWrYVS9nviK', 'FINANCEIRO'),
    ('Renata RH', 'renata.rh@corp.com', '$2b$12$lDJcdSvX2mllFyxZXDgOg.uUGP127l1xbTDXBUb50xOBbLsNmUslO', 'RH'),
    ('Otavio Operacoes', 'otavio.operacoes@corp.com', '$2b$12$RCIPS32QETrIjCrCd9B9ieroyM1v8meDzJcGZPePSU1QuZwD2eX6u', 'OPERACOES');

INSERT INTO modules (code, name, description)
VALUES
    ('PORTAL', 'Portal do Colaborador', 'Portal com informações gerais para todos os colaboradores.'),
    ('RELATORIOS', 'Relatórios Gerenciais', 'Relatórios consolidados para diferentes áreas.'),
    ('GESTAO_FINANCEIRA', 'Gestão Financeira', 'Módulo de controle financeiro corporativo.'),
    ('APROVADOR_FIN', 'Aprovador Financeiro', 'Aprovação de fluxos financeiros críticos.'),
    ('SOLICITANTE_FIN', 'Solicitante Financeiro', 'Solicitação de processos financeiros.'),
    ('ADMIN_RH', 'Administrador RH', 'Administração completa de recursos humanos.'),
    ('COLAB_RH', 'Colaborador RH', 'Acesso limitado para equipe de RH.'),
    ('ESTOQUE', 'Gestão de Estoque', 'Operação de estoque e inventário.'),
    ('COMPRAS', 'Compras', 'Gestão de pedidos de compra.'),
    ('AUDITORIA', 'Auditoria', 'Ferramentas de auditoria interna.');

-- Departamentos permitidos
INSERT INTO module_departments (module_id, department)
SELECT m.id, d.department
FROM modules m
JOIN (
    SELECT 'PORTAL' AS code, department FROM (VALUES ('TI'), ('FINANCEIRO'), ('RH'), ('OPERACOES'), ('OUTROS')) AS t(department)
    UNION ALL
    SELECT 'RELATORIOS' AS code, department FROM (VALUES ('TI'), ('FINANCEIRO'), ('RH'), ('OPERACOES'), ('OUTROS')) AS t2(department)
    UNION ALL
    SELECT 'GESTAO_FINANCEIRA' AS code, department FROM (VALUES ('TI'), ('FINANCEIRO')) AS t3(department)
    UNION ALL
    SELECT 'APROVADOR_FIN' AS code, department FROM (VALUES ('TI'), ('FINANCEIRO')) AS t4(department)
    UNION ALL
    SELECT 'SOLICITANTE_FIN' AS code, department FROM (VALUES ('TI'), ('FINANCEIRO')) AS t5(department)
    UNION ALL
    SELECT 'ADMIN_RH' AS code, department FROM (VALUES ('TI'), ('RH')) AS t6(department)
    UNION ALL
    SELECT 'COLAB_RH' AS code, department FROM (VALUES ('TI'), ('RH')) AS t7(department)
    UNION ALL
    SELECT 'ESTOQUE' AS code, department FROM (VALUES ('TI'), ('OPERACOES')) AS t8(department)
    UNION ALL
    SELECT 'COMPRAS' AS code, department FROM (VALUES ('TI'), ('OPERACOES')) AS t9(department)
    UNION ALL
    SELECT 'AUDITORIA' AS code, department FROM (VALUES ('TI')) AS t10(department)
) AS d ON m.code = d.code;

-- Incompatibilidades
INSERT INTO module_incompatibilities (module_id, incompatible_module_id)
VALUES
    ((SELECT id FROM modules WHERE code = 'APROVADOR_FIN'), (SELECT id FROM modules WHERE code = 'SOLICITANTE_FIN')),
    ((SELECT id FROM modules WHERE code = 'SOLICITANTE_FIN'), (SELECT id FROM modules WHERE code = 'APROVADOR_FIN')),
    ((SELECT id FROM modules WHERE code = 'ADMIN_RH'), (SELECT id FROM modules WHERE code = 'COLAB_RH')),
    ((SELECT id FROM modules WHERE code = 'COLAB_RH'), (SELECT id FROM modules WHERE code = 'ADMIN_RH'));
