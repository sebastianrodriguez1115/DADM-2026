## MODIFIED Requirements

### Requirement: Límites de la sesión en memoria
Un snapshot temporal de SavedStateHandle MUST restaurar únicamente recreación soportada y nunca ser almacén permanente. Sin snapshot válido, se inicia tablero vacío y X; SharedPreferences es la única autoridad de scores y dificultad, incluso tras muerte de proceso soportada. SoundEnabled se conserva solo en snapshot temporal y vuelve a true en sesión nueva. No se restaura una partida tras cierre deliberado, force-stop o nuevo lanzamiento sin snapshot; background no equivale a cierre. Código de dificultad estable: 0 Easy, 1 Hard, 2 Expert, 3 Impossible; códigos ausentes o inválidos usan Expert. Back desde la partida local vuelve a la pantalla inicial sin finalizar la tarea ni reiniciar la sesión; Back desde la pantalla inicial aplica la navegación normal del sistema y no garantiza finalizar tarea.

#### Scenario: Rotación con estado guardado
- **GIVEN** que el host se recrea por configuración con estado temporal válido
- **WHEN** se recrea la pantalla
- **THEN** se recuperan tablero, turno, resultado, iniciadores, sonido y restante O sin efectos duplicados

#### Scenario: Restauración soportada por muerte de proceso
- **GIVEN** que Android recrea el host con snapshot temporal válido
- **WHEN** se restaura
- **THEN** se restaura estado de partida y sonido de sesión, mientras scores/dificultad provienen de SharedPreferences

#### Scenario: Sin snapshot temporal
- **GIVEN** que se abre sin snapshot válido
- **WHEN** comienza el juego
- **THEN** tablero vacío, X, sonido true y scores/dificultad cargados de SharedPreferences o sus defaults

#### Scenario: Snapshot corrupto
- **GIVEN** que el snapshot tiene campos ausentes, inválidos o incompatibles
- **WHEN** se intenta restaurar
- **THEN** no hay crash ni aplicación parcial; se inicia tablero vacío con X y prefs válidas

#### Scenario: Nueva sesión tras finalizar la tarea
- **GIVEN** que finalizó la tarea y no hay snapshot válido
- **WHEN** se vuelve a abrir
- **THEN** no se restaura la partida y scores/dificultad persistidos válidos permanecen

#### Scenario: Sin persistencia permanente
- **GIVEN** que el usuario finaliza deliberadamente o fuerza detención
- **WHEN** vuelve a abrir sin snapshot temporal
- **THEN** no se restaura tablero ni turno; scores/dificultad persistidos sí

#### Scenario: Nueva sesión tras perder el proceso
- **GIVEN** que el proceso anterior se perdió y no hay snapshot temporal válido
- **WHEN** comienza una nueva sesión
- **THEN** el tablero inicia vacío con X y scores/dificultad proceden de preferencias válidas o defaults

#### Scenario: Background y Back
- **GIVEN** que se envía a background o se pulsa Back desde la partida local
- **WHEN** Android conserva la tarea
- **THEN** no se fuerza terminación ni se reinicia estado por el mero background; Back desde la partida local solo muestra la pantalla inicial con la sesión intacta

#### Scenario: Códigos de dificultad
- **GIVEN** que se carga un código entero persistido
- **WHEN** vale 0, 1, 2 o 3
- **THEN** se asigna respectivamente Easy, Hard, Expert o Impossible

#### Scenario: Código de dificultad desconocido
- **GIVEN** que el código falta o no vale 0, 1, 2 o 3
- **WHEN** se carga
- **THEN** se usa Expert sin crash
