# Spec Delta

## ADDED Requirements

### Requirement: Presentación adaptativa por orientación
La interfaz SHALL ser operable en portrait y landscape sin fijar orientación ni impedir recreación nativa. En landscape organizará un tablero cuadrado, con 270 dp como referencia adaptable a constraints, a la izquierda y estado, marcador y controles a la derecha; ante límites de espacio o fontScale podrá usar scroll o disposición compacta sin recortar contenido ni reducir objetivos bajo 48 dp.

#### Scenario: Disposición landscape
- **GIVEN** que el área horizontal tiene espacio suficiente para dos paneles legibles y targets mínimos
- **WHEN** se presenta el juego
- **THEN** el tablero cuadrado aparece a la izquierda y estado, marcador y controles se organizan a la derecha

#### Scenario: Disposición portrait
- **GIVEN** que el área disponible es vertical
- **WHEN** se presenta el juego
- **THEN** tablero, estado, marcador y controles permanecen disponibles mediante composición vertical funcional

#### Scenario: Restricciones extremas y accesibilidad
- **GIVEN** que dimensiones o fontScale impiden mostrar todo simultáneamente
- **WHEN** se adapta la presentación
- **THEN** scroll o modo compacto permite alcanzar contenido sin recortes ni objetivos interactivos menores de 48 dp

#### Scenario: Sin barra superior
- **GIVEN** que se muestra cualquiera de las orientaciones
- **WHEN** se renderiza la pantalla
- **THEN** no existe TopAppBar ni título de aplicación y el menú compacto permanece accesible junto a controles

### Requirement: Acción accesible de reset de marcador
El menú compacto SHALL incluir «Reiniciar puntajes» con icono y etiqueta textual accesible, que ponga inmediatamente a cero los tres contadores sin reiniciar ni alterar la partida.

#### Scenario: Acción en menú compacto
- **GIVEN** que se abre menú de opciones
- **WHEN** se recorre con tecnología asistiva
- **THEN** Reiniciar puntajes aparece con icono, etiqueta y acción accesible

#### Scenario: Reset conserva juego
- **GIVEN** que la partida está activa o terminal
- **WHEN** se usa Reiniciar puntajes
- **THEN** los contadores pasan inmediatamente a cero sin cambiar tablero, turno, resultado o iniciadores
