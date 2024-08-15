import { type ReactNode } from 'react';

interface Props {
  children: ReactNode
}

export default function ListCard({ children }: Props) {
  return (
    { children }
  );
}
