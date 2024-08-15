import { Text, type TextProps } from '@mantine/core';
import { type ReactNode } from 'react';

interface Props extends TextProps {
  children: ReactNode
}

export default function Error(props: Props) {
  return <Text c="red" {...props} />;
}
