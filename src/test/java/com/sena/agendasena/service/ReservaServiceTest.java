package com.sena.agendasena.service;

import com.sena.agendasena.dto.ReservaRequest;
import com.sena.agendasena.exception.ConflictoHorarioException;
import com.sena.agendasena.exception.RecursoNoEncontradoException;
import com.sena.agendasena.exception.ReglaNegocioException;
import com.sena.agendasena.model.Ambiente;
import com.sena.agendasena.model.EstadoReserva;
import com.sena.agendasena.model.Reserva;
import com.sena.agendasena.model.TipoAmbiente;
import com.sena.agendasena.repository.AmbienteRepository;
import com.sena.agendasena.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Clase de pruebas unitarias para ReservaService.
 *
 * Explicación didáctica para principiantes:
 * - @ExtendWith(MockitoExtension.class): Inicializa el framework de simulación Mockito.
 * - @Mock: Crea un objeto simulado (Mock). No va a la base de datos real.
 * - @InjectMocks: Crea una instancia real de ReservaService e inyecta los repositorios simulados.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AmbienteRepository ambienteRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Ambiente ambienteValido;
    private LocalDateTime mananaOchoAM;
    private LocalDateTime mananaDiezAM;

    /**
     * @BeforeEach: Este método se ejecuta automáticamente antes de CADA prueba individual.
     * Sirve para preparar datos de prueba limpios y comunes.
     */
    @BeforeEach
    void setUp() {
        // Creamos un ambiente activo con capacidad para 20 personas
        ambienteValido = new Ambiente("Sala 101 - Sistemas", TipoAmbiente.SALA, 20, true);
        ambienteValido.setId(1L);

        // Definimos un horario válido para mañana (para cumplir la regla de no reservar en el pasado)
        mananaOchoAM = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        mananaDiezAM = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Prueba el flujo feliz: crear una reserva válida con datos correctos.
     */
    @Test
    void crearReserva_Exito() {
        // 1. DATO DE ENTRADA: Solicitud de reserva válida
        ReservaRequest request = new ReservaRequest(
                1L,
                "Carlos Zuluaga",
                mananaOchoAM,
                mananaDiezAM,
                15 // 15 aprendices (capacidad del ambiente es 20)
        );

        // 2. STUBBING (Comportamiento simulado): 
        // - Cuando el repositorio busque el ambiente por ID, devolverá nuestro 'ambienteValido'.
        // - Cuando cuente las reservas del instructor en el día, dirá que tiene 0.
        // - Cuando busque cruces de horario, dirá que hay 0 traslapes.
        // - Cuando guarde la reserva, retornará la reserva guardada.
        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));
        when(reservaRepository.countInstructorReservationsOnDay(any(), any(), any())).thenReturn(0L);
        when(reservaRepository.countOverlappingReservations(any(), any(), any())).thenReturn(0L);
        
        // Simulamos la persistencia en base de datos devolviendo una reserva con ID
        Reserva reservaGuardada = new Reserva(ambienteValido, "Carlos Zuluaga", mananaOchoAM, mananaDiezAM, 15, EstadoReserva.ACTIVA);
        reservaGuardada.setId(100L);
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reservaGuardada);

        // 3. EJECUCIÓN: Llamamos al método real del servicio
        Reserva resultado = reservaService.crearReserva(request);

        // 4. ASERCIONES (Comprobar resultados):
        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        assertEquals(EstadoReserva.ACTIVA, resultado.getEstado());
        assertEquals("Carlos Zuluaga", resultado.getNombreInstructor());

        // Verificamos que se haya llamado efectivamente al método save del repositorio una vez
        verify(reservaRepository, times(1)).save(any(Reserva.class));
    }

    /**
     * Prueba que se lance un error si el ambiente solicitado no existe.
     */
    @Test
    void crearReserva_AmbienteNoExiste_LanzaExcepcion() {
        ReservaRequest request = new ReservaRequest(999L, "Carlos Zuluaga", mananaOchoAM, mananaDiezAM, 10);
        
        // Simulamos que el ambiente con ID 999 no existe (retorna Optional vacío)
        when(ambienteRepository.findById(999L)).thenReturn(Optional.empty());

        // Validamos que se lance RecursoNoEncontradoException
        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.crearReserva(request));
    }

    /**
     * Regla 1: Conflicto de horario (solapamiento).
     */
    @Test
    void crearReserva_Regla1_CrucesDeHorario_LanzaExcepcion() {
        ReservaRequest request = new ReservaRequest(1L, "Juan Perez", mananaOchoAM, mananaDiezAM, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));
        // Simulamos que ya hay 1 reserva existente que se cruza en ese rango
        when(reservaRepository.countOverlappingReservations(1L, mananaOchoAM, mananaDiezAM)).thenReturn(1L);

        // Validamos que lance ConflictoHorarioException
        ConflictoHorarioException ex = assertThrows(ConflictoHorarioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Conflicto de horario"));
    }

    /**
     * Regla 2: La cantidad de aprendices supera la capacidad del ambiente.
     */
    @Test
    void crearReserva_Regla2_SuperaCapacidad_LanzaExcepcion() {
        // Pedimos 25 aprendices en un ambiente que solo tiene capacidad para 20
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", mananaOchoAM, mananaDiezAM, 25);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Capacidad superada"));
    }

    /**
     * Regla 3: Horario institucional (antes de las 6:00 AM).
     */
    @Test
    void crearReserva_Regla3_AntesDeLasSeis_LanzaExcepcion() {
        LocalDateTime cincoAM = mananaOchoAM.withHour(5);
        LocalDateTime sieteAM = mananaOchoAM.withHour(7);
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", cincoAM, sieteAM, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Fuera de horario"));
    }

    /**
     * Regla 3: Horario institucional (después de las 22:00 / 10:00 PM).
     */
    @Test
    void crearReserva_Regla3_DespuesDeLasDiezPM_LanzaExcepcion() {
        LocalDateTime nuevePM = mananaOchoAM.withHour(21);
        LocalDateTime oncePM = mananaOchoAM.withHour(23);
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", nuevePM, oncePM, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Fuera de horario"));
    }

    /**
     * Regla 3: Debe iniciar y terminar el mismo día.
     */
    @Test
    void crearReserva_Regla3_DiferenteDia_LanzaExcepcion() {
        LocalDateTime inicioHoy = mananaOchoAM;
        LocalDateTime finManana = mananaOchoAM.plusDays(1).withHour(10);
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", inicioHoy, finManana, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Fechas inválidas o no coinciden"));
    }

    /**
     * Regla 3: Duración menor a 1 hora (menos de 60 minutos).
     */
    @Test
    void crearReserva_Regla3_DuracionMenorUnaHora_LanzaExcepcion() {
        LocalDateTime inicio = mananaOchoAM;
        LocalDateTime fin = mananaOchoAM.plusMinutes(45); // 45 minutos de duración
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", inicio, fin, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Duración entre 1 y 4 horas"));
    }

    /**
     * Regla 3: Duración mayor a 4 horas (más de 240 minutos).
     */
    @Test
    void crearReserva_Regla3_DuracionMayorCuatroHoras_LanzaExcepcion() {
        LocalDateTime inicio = mananaOchoAM;
        LocalDateTime fin = mananaOchoAM.plusHours(5); // 5 horas de duración
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", inicio, fin, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Duración entre 1 y 4 horas"));
    }

    /**
     * Regla 4: El ambiente está inactivo (activo = false).
     */
    @Test
    void crearReserva_Regla4_AmbienteInactivo_LanzaExcepcion() {
        ambienteValido.setActivo(false); // Desactivamos el ambiente
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", mananaOchoAM, mananaDiezAM, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Ambiente inactivo"));
    }

    /**
     * Regla 5: Límite por instructor superado (más de 3 reservas activas en el mismo día).
     */
    @Test
    void crearReserva_Regla5_InstructorExcedeReservas_LanzaExcepcion() {
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", mananaOchoAM, mananaDiezAM, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));
        // Simulamos que el instructor ya cuenta con 3 reservas activas registradas para ese día
        when(reservaRepository.countInstructorReservationsOnDay(eq("Carlos Zuluaga"), any(), any())).thenReturn(3L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Límite de 3 reservas diario alcanzado"));
    }

    /**
     * Regla 6: Cancelación exitosa de una reserva activa en el futuro.
     */
    @Test
    void cancelarReserva_Exito() {
        // Una reserva programada para pasado mañana (muy en el futuro para que falten > 2 horas)
        LocalDateTime inicioReserva = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0);
        Reserva reserva = new Reserva(ambienteValido, "Carlos Zuluaga", inicioReserva, inicioReserva.plusHours(2), 10, EstadoReserva.ACTIVA);
        reserva.setId(5L);

        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

        Reserva resultado = reservaService.cancelarReserva(5L);

        assertNotNull(resultado);
        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());
        verify(reservaRepository, times(1)).save(reserva);
    }

    /**
     * Regla 6: Error al cancelar porque faltan menos de 2 horas para su inicio.
     */
    @Test
    void cancelarReserva_FaltaMenosDosHoras_LanzaExcepcion() {
        // Reservado para iniciar en solo 1 hora
        LocalDateTime inicioReserva = LocalDateTime.now().plusHours(1);
        Reserva reserva = new Reserva(ambienteValido, "Carlos Zuluaga", inicioReserva, inicioReserva.plusHours(2), 10, EstadoReserva.ACTIVA);
        reserva.setId(3L);

        when(reservaRepository.findById(3L)).thenReturn(Optional.of(reserva));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.cancelarReserva(3L));
        assertTrue(ex.getMessage().contains("Anticipación mínima de 2 horas requerida"));
    }

    /**
     * Regla 7: Intentar reservar en una fecha/hora del pasado.
     */
    @Test
    void crearReserva_Regla7_FechaPasada_LanzaExcepcion() {
        // Una fecha y hora de inicio de ayer
        LocalDateTime ayerOchoAM = LocalDateTime.now().minusDays(1).withHour(8);
        LocalDateTime ayerDiezAM = LocalDateTime.now().minusDays(1).withHour(10);
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", ayerOchoAM, ayerDiezAM, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("No puedes reservar en el pasado"));
    }

    /**
     * Validación preventiva: Hora de inicio posterior a la de fin.
     */
    @Test
    void crearReserva_InicioDespuesDeFin_LanzaExcepcion() {
        // Pone las 10:00 como inicio y las 08:00 como fin
        ReservaRequest request = new ReservaRequest(1L, "Carlos Zuluaga", mananaDiezAM, mananaOchoAM, 10);

        when(ambienteRepository.findById(1L)).thenReturn(Optional.of(ambienteValido));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.crearReserva(request));
        assertTrue(ex.getMessage().contains("Fechas inválidas o no coinciden"));
    }
}
