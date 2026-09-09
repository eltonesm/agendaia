-- Status de pagamento (IDEA-006/gestao-de-clientes): independente de
-- status (AppointmentStatus) -- pergunta diferente ("foi pago?" vs. "o
-- atendimento aconteceu?"). PENDING e o padrao para todo agendamento novo;
-- linhas ja existentes recebem o mesmo padrao (o dono corrige manualmente
-- as que lembrar).
ALTER TABLE appointment
  ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
