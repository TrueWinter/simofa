import { CopyButton as CB, ActionIcon, Tooltip, rem, CopyButtonProps } from '@mantine/core';
import { IconCopy, IconCheck } from '@tabler/icons-react';

type Props = Omit<CopyButtonProps, 'children'>;

export default function CopyButton(props: Props) {
  return (
    <CB timeout={2000} {...props}>
      {({ copied, copy }) => (
        <Tooltip label={copied ? 'Copied' : 'Copy'} withArrow position="right">
          <ActionIcon color={copied ? 'teal' : 'gray'} variant="subtle" onClick={copy}>
            {copied ? (
              <IconCheck style={{ width: rem(16) }} />
            ) : (
              <IconCopy style={{ width: rem(16) }} />
            )}
          </ActionIcon>
        </Tooltip>
      )}
    </CB>
  );
}
