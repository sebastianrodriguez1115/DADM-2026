# Observación de tooling QA — corregida antes del reporte

Primera corrida Android del reintento: `android-retry-20261003T124759114559Z/`.

Los cuatro comandos Android terminaron con exit 0 y XML con 4/7/6/1 tests, cero fallos/error/skip; JVM exit 0 y 108 tests. Sin embargo el **wrapper QA terminó con exit 1** y sus estados Android `FAIL` son incorrectos: el parser buscaba hijos `testcase` directos en el documento raíz, mientras los XML Android tienen estructura **`testsuites/testsuite/testcase`**. Las listas `cases` quedaron vacías, disparando el guard de conteo. No es fallo de app ni de los tests instrumentados.

Se corrigieron ambos scripts QA para recorrer `suite.iter("testcase")`, que soporta raíces JVM `testsuite` y Android `testsuites`. No se modificó código ni tests del producto. La primera evidencia y sus estados incorrectos se preservan sin sobrescribir para trazabilidad; una segunda corrida independiente se utiliza para el reporte final.
