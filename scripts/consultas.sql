-- =====================================================================
--  Consultas para o vídeo: rodar no Query Editor do banco db-dimdim
--  (Portal Azure > SQL databases > db-dimdim > Query editor)
--  Execute depois de CADA operação do CRUD para mostrar a persistência
-- =====================================================================

-- Tabela CLIENTE
SELECT * FROM dbo.cliente ORDER BY id;

-- Tabela CONTA
SELECT * FROM dbo.conta ORDER BY id;

-- Relacionamento 1:N (cliente -> contas)
SELECT c.id   AS cliente_id,
       c.nome,
       c.cpf,
       ct.id  AS conta_id,
       ct.agencia,
       ct.numero,
       ct.tipo,
       ct.saldo
  FROM dbo.cliente c
  LEFT JOIN dbo.conta ct ON ct.cliente_id = c.id
 ORDER BY c.id, ct.numero;
