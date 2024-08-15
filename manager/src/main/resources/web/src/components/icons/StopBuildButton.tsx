import { IconHandStop } from '@tabler/icons-react';
import { useState } from 'react';
import Icon, { IconProps } from '../Icon';
import { deleteModal } from '../../util/modals';

interface Props extends Omit<IconProps, 'label'> {
  status: string
  websiteId: string
  buildId: string
}

export default function StopBuildButton({ status, websiteId, buildId, ...props }: Props) {
  const [busy, setBusy] = useState(false);

  return (
    <Icon label="Stop Build" color="red" loading={busy} {...props}
      disabled={!['QUEUED', 'PREPARING', 'BUILDING'].includes(status)}
      onClick={() => {
        deleteModal({
          type: 'build',
          url: `/api/websites/${websiteId}/builds/${buildId}`,
          uuid: buildId,
          onConfirm: () => setBusy(true),
          cb: () => setBusy(false),
          action: {
            present: 'stop',
            past: 'stopped'
          }
        });
      }}>
      <IconHandStop />
    </Icon>
  );
}
