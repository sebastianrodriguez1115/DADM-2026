# game-sound-effects Specification

## Purpose

Define los efectos sonoros diferenciados de las jugadas aceptadas y el control de sonido limitado a la sesión actual.

## Requirements

### Requirement: Efectos de jugadas aceptadas
Para cada movimiento aceptado, el sistema MUST intentar como máximo una reproducción de su efecto correspondiente, con efectos distintos para X y O; se incluyen las jugadas terminales y la apertura de O. Solo SHALL intentar reproducir cuando esté en primer plano, el sonido habilitado y el recurso listo. Si no está listo o falla, MUST omitirlo sin cola ni reintento tardío; los fallos MUST NOT interrumpir el juego.

#### Scenario: Efecto correspondiente cuando está disponible
- **GIVEN** que el recurso de audio está listo y el sonido habilitado en primer plano
- **WHEN** se acepta un movimiento de X u O, incluso terminal o apertura
- **THEN** se reproduce exactamente una vez el efecto correspondiente, distinto para X y O

#### Scenario: Sin sonido en entrada rechazada o cambio visual
- **WHEN** una entrada se rechaza, se repinta o se recrea la pantalla sin aceptar jugada
- **THEN** no se reproduce ningún efecto de jugada

#### Scenario: Recurso no listo o fallo sin reintento
- **GIVEN** que el recurso no está listo o la reproducción falla
- **WHEN** se acepta un movimiento
- **THEN** se omite el efecto sin encolarlo ni reintentarlo después, y el estado y flujo del juego continúan

#### Scenario: Sin audio cuando no se cumplen condiciones
- **GIVEN** que el juego está en segundo plano o el sonido deshabilitado
- **WHEN** se acepta un movimiento
- **THEN** no se intenta reproducir ni se encola el efecto

#### Scenario: Mutear detiene reproducción activa
- **GIVEN** que un efecto está sonando
- **WHEN** se deshabilita el sonido
- **THEN** se detiene el sonido activo y no se reproducen efectos posteriores mientras siga deshabilitado

#### Scenario: Perder primer plano detiene y descarta audio activo
- **GIVEN** que un efecto está sonando o pendiente de inicio
- **WHEN** el juego pierde primer plano
- **THEN** se detiene el sonido activo y se descarta cualquier reproducción pendiente sin reanudarla al volver

#### Scenario: Sin audio por rechazo, cancelación o recreación
- **GIVEN** que no se acepta un movimiento, se cancela un turno de O, o solo ocurre recomposición, recreación o reanudación
- **WHEN** sucede cualquiera de esos eventos
- **THEN** no se reproduce ni se reintenta ningún efecto

### Requirement: Opción de sonido en la sesión
El sonido SHALL iniciar habilitado en cada sesión y SHALL poder habilitarse o deshabilitarse durante la partida. La preferencia SHALL conservarse al iniciar otra partida, rotar o pasar a segundo plano, y SHALL NOT persistir entre sesiones.

#### Scenario: Estado inicial y cambio de opción
- **WHEN** comienza una sesión nueva y se consulta o cambia la opción
- **THEN** el sonido inicia habilitado y la persona puede alternarlo durante la partida

#### Scenario: Retener preferencia en la sesión
- **GIVEN** que la persona deshabilitó el sonido
- **WHEN** inicia otra partida, rota o vuelve de segundo plano en la sesión viva
- **THEN** el sonido permanece deshabilitado

#### Scenario: Preferencia nueva en sesión nueva
- **GIVEN** que comienza una sesión nueva
- **WHEN** se consulta la opción de sonido
- **THEN** está habilitada sin restaurar una preferencia anterior
