import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

/**
 * Limpa o DOM depois de CADA teste.
 *
 * A Testing Library só registra essa limpeza sozinha quando encontra um `afterEach`
 * GLOBAL - o que acontece no Jest, ou no Vitest com `test.globals: true`. Como aqui
 * importamos describe/it/expect explicitamente (globals desligado), a limpeza precisa
 * ser registrada à mão. Sem ela, o componente renderizado num teste continua no DOM
 * durante o teste seguinte (ex: dois `data-testid="charged-value"` na tela).
 */
afterEach(() => {
  cleanup();
});
