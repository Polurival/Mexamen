# S2 · Tarjetas didácticas (flashcards)

**Цель:** пройти сессию карточек: перевернуть карту и оценить себя.
**Предусловие:** чистое состояние.

```steps
clear
launch
tap: Comenzar tarjetas didácticas
expect: Todos los temas
tap: Todos los temas
expect: Tarjeta 1 de 10
expect: Toca para ver la respuesta
repeat 10:
  tap: Toca para ver la respuesta
  expect: Bien
  tap: Bien
expect: ¡Sesión completada!
tap: Volver al inicio
expect: Temas de Tarjetas
back
expect: Prepárate para el examen de naturalización
```

**Ожидание:** каждая карта переворачивается, появляются кнопки «Otra vez / Difícil / Bien / Fácil»; после 10 карт — «¡Sesión completada!»; «Volver al inicio» закрывает сессию и возвращает к списку тем (штатное поведение), «назад» — на главную.
