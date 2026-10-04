# Corrección del harness exploratorio

Primera ejecución: las cinco comprobaciones previas a terminal pasaron. El harness exigía que la partida terminal conservase casillas vacías, pero la partida real terminó correctamente en empate con nueve marcas. Esta era una precondición errónea del test, no un defecto del producto.

Se corrigió sólo `explore-adb.py` para aceptar empate o victoria y registrar cuántas vacías quedan; se reejecuta desde nueva sesión. El rechazo terminal de nueve casillas vacías está cubierto independientemente por `GameBoardInteractionTest.computerTurnAndTerminalStateExposeDisabledCellsWithoutCallbacks` (PASS). No se modificó app ni tests del proyecto.
