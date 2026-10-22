# S4 · Cancelar reinicio de estadísticas

**Цель:** диалог сброса не стирает данные при «Cancelar».
**Предусловие:** чистое состояние.

```steps
clear
launch
tap: Comenzar quiz
tap: Todos los temas
tap_option: 1
expect: Siguiente
tap: Cerrar quiz
launch
tap: Estadísticas
tap: Reiniciar estadísticas
expect: ¿Reiniciar estadísticas?
tap: Cancelar
expect_absent: ¿Reiniciar estadísticas?
expect_text_value: Preguntas intentadas = 1
```

**Ожидание:** после «Cancelar» диалог закрыт, счётчик «Preguntas intentadas» по-прежнему 1.
