import { ReactNode, useEffect } from 'react';

interface Props {
  title?: string
  children: ReactNode
}

export default function Page({ title, children }: Props) {
  useEffect(() => {
    document.title = title ? `${title} | Simofa` : 'Simofa';
  }, []);

  return children;
}
