# S5 · Leyendas: список и детальный экран

**Цель:** открыть список легенд, зайти в одну и вернуться.
**Предусловие:** чистое состояние.

```steps
clear
launch
tap: Leyendas
expect: Leyendas Mexicanas
tap: El cenote Zací
expect_absent: Leyendas Mexicanas
back
expect: Leyendas Mexicanas
back
expect: Comenzar quiz
```

**Ожидание:** список легенд открывается, легенда показывается отдельным экраном, системная «назад» ведёт обратно в список, затем на главную.
