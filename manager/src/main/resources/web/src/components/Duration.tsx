import { useEffect, useRef, useState } from 'react';
import { Text, Tooltip } from '@mantine/core';
import { parseTime } from '../util/time';

interface Props {
  time: number
  status: string
}

export default function Duration({ time, status }: Props) {
  const [state, setState] = useState(time);
  const interval = useRef(null);
  const shouldIncrement = useRef(false);

  if (['BUILDING', 'DEPLOYING'].includes(status)) {
    shouldIncrement.current = true;
  }

  if (time > state) {
    setState(time);
  }

  if (!['QUEUED', 'PREPARING', 'BUILDING', 'DEPLOYING'].includes(status)) {
    clearInterval(interval.current);
    if (time !== state) {
      setState(time);
    }
  }

  useEffect(() => {
    interval.current = setInterval(() => {
      if (shouldIncrement.current) {
        setState((s) => s + 1000);
      }
    }, 1000);
    return () => clearInterval(interval.current);
  }, []);

  const estimated = Math.floor(state / 1000) !== Math.floor(time / 1000);
  const parsedTime = parseTime(state);

  return estimated ? (
    <Tooltip label="Estimated duration">
      <Text component="span">{parsedTime}</Text>
    </Tooltip>
  ) : parsedTime;
}
