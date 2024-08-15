import { useEffect, useState } from 'react';
import { ActionIcon, Anchor, Group, Table, Text } from '@mantine/core';
import { IconFolderOpen } from '@tabler/icons-react';
import { UseFormReturnType } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import type { Template, Website } from '../types/java';
import { get, post } from '../util/api';
import TableSkeleton from './TableSkeleton';
import ShortUuid from './ShortUuid';
import DeleteButton from './icons/DeleteButton';

export interface TemplatesProps {
  form: UseFormReturnType<Omit<Website, 'id'>>
  close: () => void
}

export default function Templates({
  form, close
}: TemplatesProps) {
  const [templates, setTemplates] = useState([] as Template[]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  function loadTemplates() {
    setLoading(true);

    get('/api/templates').then((d) => {
      if (d.status !== 200) {
        setError(d.body.title || 'An error occurred');
        return;
      }

      setTemplates(d.body.templates);
      setLoading(false);
    });
  }

  useEffect(() => {
    loadTemplates();
  }, []);

  function loadTemplate(e: Template) {
    form.setValues(JSON.parse(e.template));
    close();
  }

  function handleError(msg?: string, hideAfter?: number) {
    notifications.show({
      color: 'red',
      message: `Failed to save template: ${msg || 'An error occurred'}`,
      autoClose: hideAfter || undefined
    });
    setSaving(false);
    close();
  }

  function saveCurrent() {
    setSaving(true);

    const values = form.getValues();
    const { name } = values;
    const data = {
      dockerImage: values.dockerImage,
      memory: values.memory,
      cpu: values.cpu,
      buildCommand: values.buildCommand,
      deployCommand: values.deployCommand,
      deployFailedCommand: values.deployFailedCommand
    };

    if (name.trim() === '' || !Object.values(data).every((e) => e !== '')) {
      handleError('the following fields are required: name, Docker image, memory, CPU, ' +
        'build command, deploy command, deploy failed command', 10 * 1000);
      return;
    }

    const template = JSON.stringify(data);

    if (template.length >= 4000) {
      handleError('4000 character limit exceeded');
      return;
    }

    post('/api/templates', {
      name,
      template
    }).then((b) => {
      if (b.status !== 200) {
        handleError(b.body.title);
        return;
      }

      notifications.show({
        message: 'Template saved'
      });
      loadTemplates();
      setSaving(false);
    });
  }

  return (
    <>
      {saving ? <Text>Saving...</Text> : (
        <Anchor onClick={() => saveCurrent()}>
          Save current form data as template</Anchor>
      )}
      {error ? <Text c="red">{error}</Text> : (
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>ID</Table.Th>
              <Table.Th>Name</Table.Th>
              <Table.Th>Actions</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {loading ? <TableSkeleton columns={3} /> :
              templates.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td><ShortUuid uuid={e.id} /></Table.Td>
                  <Table.Td>{e.name}</Table.Td>
                  <Table.Td>
                    <Group>
                      <ActionIcon onClick={() => loadTemplate(e)}>
                        <IconFolderOpen />
                      </ActionIcon>
                      <DeleteButton opts={{
                        type: 'template',
                        uuid: e.id,
                        url: `/api/templates/${e.id}`,
                        cb: loadTemplates
                      }} />
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
          </Table.Tbody>
        </Table>
      )}
    </>
  );
}
