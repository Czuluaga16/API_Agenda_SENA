
INSERT INTO ambientes (nombre, tipo, capacidad, activo) VALUES 
('Sala Sistemas 1', 'SALA', 20, true),
('Laboratorio Hardware', 'LABORATORIO', 15, true),
('Auditorio Principal', 'AUDITORIO', 50, true),
('Sala Multimedia', 'SALA', 10, false);


INSERT INTO reservas (ambiente_id, nombre_instructor, fecha_hora_inicio, fecha_hora_fin, numero_aprendices, estado) VALUES 
(1, 'Carlos Zuluaga', '2026-06-25T08:00:00', '2026-06-25T10:00:00', 15, 'ACTIVA');