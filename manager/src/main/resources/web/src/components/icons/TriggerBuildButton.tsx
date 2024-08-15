import { notifications } from '@mantine/notifications';
import { IconHammer } from '@tabler/icons-react';
import { useState } from 'react';
import type { Website } from '../../types/java';
import Icon, { type IconProps } from '../Icon';

interface Props extends Omit<IconProps, 'label'> {
  website: Website
}

export default function TriggerBuild({ website, ...props }: Props) {
  const [busy, setBusy] = useState(false);
  const commitMsg = encodeURIComponent('<manual build>');

  return (
    <Icon label="Trigger Build" loading={busy} {...props} onClick={() => {
      setBusy(true);
      fetch(`/public-api/deploy-hook?website=${website.id}&token=${website.deployToken}` +
        `&commit=${commitMsg}`, {
        method: 'POST',
        headers: {
          accept: 'application/json'
        }
      }).then((resp) => {
        if (resp.status !== 200) {
          throw new Error(`Received non-200 status code: ${resp.status}`);
        }

        notifications.show({
          message: 'Build triggered'
        });
      }).catch((e) => {
        notifications.show({
          color: 'red',
          message: `Failed to trigger build: ${e}`
        });
      }).finally(() => {
        setBusy(false);
      });
    }}>
      <IconHammer />
    </Icon>
  );
}
