import { Box, Input, Skeleton, Stack, TextInput } from '@mantine/core';
import { useRef } from 'react';

function randomPercent() {
  const MIN = 5;
  const MAX = 20;

  return MIN + Math.floor(Math.random() * (MAX - MIN));
}

interface InputProps {
  w?: string | number
  lw?: string | number
}

function InputSkeleton(props: InputProps) {
  const percent = useRef(randomPercent());

  return (
    <Box w={props.w}>
      <Skeleton w={props.lw || `${percent.current}%`}
        h="md" mb="calc(var(--mantine-spacing-xs) / 2)">
        <Input.Label>Test Input</Input.Label>
      </Skeleton>
      <Skeleton>
        <TextInput />
      </Skeleton>
    </Box>
  );
}

interface Props extends InputProps {
  n?: number
}

export default function FormSkeleton(props: Props) {
  return (
    <Stack>
      {/* eslint-disable-next-line react/no-array-index-key */}
      {new Array(props.n || 5).fill(null).map((_, i) => <InputSkeleton key={i} {...props} />)}
    </Stack>
  );
}
