# S3 · Estadísticas reflejan respuestas y se reinician

**Цель:** ответ в квизе отражается в статистике; сброс обнуляет счётчики.
**Предусловие:** чистое состояние.

```steps
clear
launch
tap: Comenzar quiz
tap: Todos los temas
expect: Pregunta 1 de 10
tap_option: 1
expect: Siguiente
tap: Cerrar quiz
launch
tap: Estadísticas
expect: Preguntas totales
expect: Preguntas intentadas
expect: Respuestas correctas
expect_text_value: Preguntas intentadas = 1
tap: Reiniciar estadísticas
expect: ¿Reiniciar estadísticas?
tap: Reiniciar
expect: Preguntas intentadas
expect_text_value: Preguntas intentadas = 0
```

**Ожидание:** после одного ответа «Preguntas intentadas» = 1; после подтверждения сброса = 0.
