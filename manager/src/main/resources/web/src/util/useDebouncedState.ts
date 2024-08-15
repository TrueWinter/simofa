import { useEffect, useRef, useState } from 'react';

export function useDebouncedState<T>(initialState: T, interval: number = 100):
  // eslint-disable-next-line no-undef
  [T, React.Dispatch<React.SetStateAction<T>>] {
  const [state, set] = useState<T>(initialState);
  const pendingState = useRef<T>(initialState);

  useEffect(() => {
    const int = setInterval(() => {
      set((s) => {
        if (Object.is(s, pendingState.current)) return s;
        return pendingState.current;
      });
    }, interval);

    return () => {
      clearInterval(int);
    };
  }, []);

  function setState(s: T | (() => T)) {
    // @ts-ignore
    pendingState.current = typeof s === 'function' ? s(pendingState.current) : s;
  }

  return [state, setState];
}
