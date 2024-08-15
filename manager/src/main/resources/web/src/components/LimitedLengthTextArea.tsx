import { Box, Text, Textarea, TextareaProps } from '@mantine/core';
import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react';

interface LengthProps {
  length: number
  maxLength: number
}

function Length({ length, maxLength }: LengthProps) {
  return (
    <Text ta="right" c="dimmed" size="sm">{length} / {maxLength}</Text>
  );
}

export default forwardRef((props: TextareaProps, outerRef) => {
  const ref = useRef<HTMLTextAreaElement>(null);
  const [length, setLength] = useState(0);

  // https://stackoverflow.com/a/77055616
  useImperativeHandle(outerRef, () => ref.current!, []);

  useEffect(() => {
    function handleChange(e: Event) {
      const target = e.target as HTMLTextAreaElement;
      setLength(target.value.length);
    }

    setLength(ref.current.value.length);

    ref.current?.addEventListener('keydown', handleChange);
    return () => ref.current?.removeEventListener('keydown', handleChange);
  }, []);

  return (
    <Box>
      <Textarea {...props} ref={ref}
        rightSection={<Length length={length} maxLength={props.maxLength} />}
        rightSectionProps={{
          style: {
            alignItems: 'flex-end',
            padding: '4px'
          }
        }} rightSectionWidth="fit-content" />
    </Box>
  );
});
