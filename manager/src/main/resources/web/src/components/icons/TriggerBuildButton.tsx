import { notifications } from '@mantine/notifications';
import { IconHammer } from '@tabler/icons-react';
import { useState } from 'react';
import { Menu } from '@mantine/core';
import type { Website } from '../../types/java';
import Icon, { type IconProps } from '../Icon';

interface Props extends Omit<IconProps, 'label'> {
  website: Website
}

export default function TriggerBuild({ website, ...props }: Props) {
  const [busy, setBusy] = useState(false);

  function triggerBuild(cache: boolean) {
    let commitMsg = encodeURIComponent('<manual build>');
    if (!cache) {
      commitMsg = `[no cache] ${commitMsg}`;
    }

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
  }

  return (
    <Icon label="Trigger Build" loading={busy} {...props}>
      <Menu withArrow>
        <Menu.Target>
          <IconHammer />
        </Menu.Target>

        <Menu.Dropdown>
          <Menu.Item onClick={() => triggerBuild(true)}>With Cache</Menu.Item>
          <Menu.Item onClick={() => triggerBuild(false)}>Without Cache</Menu.Item>
        </Menu.Dropdown>
      </Menu>
    </Icon>
  );
}
