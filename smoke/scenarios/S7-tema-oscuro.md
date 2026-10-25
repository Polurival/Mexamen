# S7 · Tema: Sistema → Claro → Oscuro con persistencia

**Цель:** переключить тему иконкой на главной по циклу и убедиться, что выбор переживает перезапуск приложения.
**Предусловие:** чистое состояние (по умолчанию — «Sistema»).

```steps
clear
launch
expect: Tema: Sistema. Cambiar a Claro
tap: Tema: Sistema. Cambiar a Claro
expect: Tema: Claro. Cambiar a Oscuro
tap: Tema: Claro. Cambiar a Oscuro
expect: Tema: Oscuro. Cambiar a Sistema
launch
expect: Tema: Oscuro. Cambiar a Sistema
tap: Tema: Oscuro. Cambiar a Sistema
expect: Tema: Sistema. Cambiar a Claro
```

**Ожидание:** contentDescription кнопки отражает текущий режим и следующий; после перезапуска сохраняется «Oscuro». Фактические цвета словарём шагов не проверяются — смотри скриншоты отчёта (после шага с «Oscuro» фон тёмный).
