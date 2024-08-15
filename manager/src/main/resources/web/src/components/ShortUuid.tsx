import { Text, Tooltip } from '@mantine/core';

interface Props {
  uuid: string
  brackets?: boolean
}

export default function ShortUuid({ uuid, brackets }: Props) {
  const shortUuid = uuid.split('-').reverse()[0];
  const renderedUuid = brackets ? `[${shortUuid}]` : shortUuid;

  return (
    <Tooltip label={uuid}>
      <Text component="span" inherit>{renderedUuid}</Text>
    </Tooltip>
  );
}
