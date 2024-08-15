import { Modal } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { forwardRef, useImperativeHandle } from 'react';
import Templates, { TemplatesProps } from './Templates';

export interface TemplateModalHandle {
  open: () => void
}

type Props = Omit<TemplatesProps, 'close'>

export default forwardRef<TemplateModalHandle, Props>((props, ref) => {
  const [opened, { open, close }] = useDisclosure(false);

  useImperativeHandle(ref, () => ({
    open
  }));

  return (
    <Modal title="Templates" opened={opened} onClose={close}>
      <Templates {...props} close={close} />
    </Modal>
  );
});
