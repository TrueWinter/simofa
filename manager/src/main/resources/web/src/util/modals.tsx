import { Text } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import ShortUuid from '../components/ShortUuid';
import { del } from './api';
import { upperCaseFirstLetter } from './text';

export interface DeleteModalOpts {
  url: string
  uuid: string
  type: string
  action?: {
    present: string
    past: string
  }
  onConfirm?: () => void
  cb?: () => void
}

export function deleteModal(opts: DeleteModalOpts) {
  const action = opts.action?.present || 'delete';
  const actionUpperCase = upperCaseFirstLetter(action);
  const pastAction = opts.action?.past || 'deleted';
  const pastActionUpperCase = upperCaseFirstLetter(pastAction);

  modals.openConfirmModal({
    title: `${actionUpperCase} ${opts.type}`,
    children: (
      <Text>
        Are you sure you want to {action} the {opts.type} with ID <ShortUuid uuid={opts.uuid} />?
      </Text>
    ),
    labels: {
      confirm: actionUpperCase,
      cancel: 'Cancel'
    },
    onConfirm: () => {
      (opts.onConfirm || (() => {}))();
      del(opts.url, {
        showProgress: true
      }).then((resp) => {
        if (resp.status !== 200) {
          notifications.show({
            message: (
              <Text>Failed to {action} {opts.type} with ID <ShortUuid uuid={opts.uuid} /></Text>
            ),
            color: 'red'
          });

          return;
        }

        notifications.show({
          message: (
            <Text>{pastActionUpperCase} {opts.type} with ID <ShortUuid uuid={opts.uuid} /></Text>
          )
        });
        (opts.cb || (() => {}))();
      });
    },
    confirmProps: {
      color: 'red'
    }
  });
}
