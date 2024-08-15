import { IconTrash } from '@tabler/icons-react';
import type { Optional } from 'utility-types';
import { useState } from 'react';
import { type DeleteModalOpts, deleteModal } from '../../util/modals';
import Icon, { type IconProps } from '../Icon';

interface Props extends Optional<IconProps, 'label'> {
  opts: DeleteModalOpts
}

export default function DeleteButton({ opts, ...props }: Props) {
  const [busy, setBusy] = useState(false);

  const onConfirm = () => {
    setBusy(true);
    (opts.onConfirm || (() => {}))();
  };

  const cb = () => {
    setBusy(false);
    (opts.cb || (() => {}))();
  };

  return (
    <Icon label="Delete" color="red" loading={busy} onClick={() => {
      deleteModal({
        ...opts,
        cb,
        onConfirm
      });
    }} {...props}>
      <IconTrash />
    </Icon>
  );
}
