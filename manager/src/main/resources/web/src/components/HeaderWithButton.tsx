import { Group, Title } from '@mantine/core';
import { ReactNode } from 'react';

export interface HeaderWithButtonProps {
  title: ReactNode
  children: ReactNode
}

export default function HeaderWithButton(props: HeaderWithButtonProps) {
  return (
    <Group justify="space-between">
      <Title w={{
        base: 'min-content',
        xs: 'inherit'
      }} miw="80%">{props.title}</Title>
      {props.children}
    </Group>
  );
}
