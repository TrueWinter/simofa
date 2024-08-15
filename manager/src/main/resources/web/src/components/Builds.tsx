import { Group, Table, Text } from '@mantine/core';
import { useEffect, useRef, useState } from 'react';
import { notifications } from '@mantine/notifications';
import type Build from '../types/Build';
import { ws as wsocket } from '../util/api';
import TableSkeleton from './TableSkeleton';
import ShortUuid from './ShortUuid';
import Duration from './Duration';
import LogsButton from './icons/LogsButton';
import StopBuildButton from './icons/StopBuildButton';

interface Props {
  website?: string
}

export default function Builds({ website }: Props) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [builds, setBuilds] = useState<Build[]>([]);
  // const interval = useRef(null);
  const ws = useRef<WebSocket>(null);

  function stopUpdating() {
    if (ws.current) {
      ws.current.close();
    }
  }

  async function loadBuilds() {
    setLoading(true);

    try {
      if (!ws.current) {
        ws.current = await wsocket(
          `/api/ws/queue${website ? `?website=${website}` : ''}`,
          `BUILD_QUEUE${website ? `-${website}` : ''}`
        );
        ws.current.onerror = () => {
          notifications.show({
            message: 'An error occured with the WebSocket connection. ' +
              'Queue will not automatically update',
            color: 'red'
          });
          ws.current.close();
          setLoading(false);
        };
        ws.current.onmessage = (event) => {
          const eventData = JSON.parse(event.data);
          const { type, data } = eventData;
          // eslint-disable-next-line default-case
          switch (type) {
            case 'queue':
              setBuilds(data);
              break;
            case 'error':
              setError(data);
              break;
          }
          setLoading(false);
        };
      }
    } catch (e) {
      setError(e.message);
      stopUpdating();
      setLoading(false);
    }
  }

  useEffect(() => {
    loadBuilds();

    return stopUpdating;
  }, []);

  return error ? <Text c="red">{error}</Text> : (
    <Table.ScrollContainer minWidth={500}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Build</Table.Th>
            <Table.Th>Website</Table.Th>
            <Table.Th>Commit Message</Table.Th>
            <Table.Th>Status</Table.Th>
            <Table.Th>Duration</Table.Th>
            <Table.Th>Actions</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {loading ? <TableSkeleton columns={6} /> : builds.map((e) => (
            <Table.Tr key={e.id}>
              <Table.Td><ShortUuid uuid={e.id} /></Table.Td>
              <Table.Td><ShortUuid uuid={e.website.id} brackets /> {e.website.name}</Table.Td>
              <Table.Td>{e.commit}</Table.Td>
              <Table.Td>{e.status.toLowerCase()}</Table.Td>
              <Table.Td><Duration time={e.runTime} status={e.status} /></Table.Td>
              <Table.Td>
                <Group justify="center">
                  <LogsButton to={`/websites/${e.website.id}/builds/${e.id}/logs`} />
                  <StopBuildButton status={e.status} websiteId={e.website.id} buildId={e.id} />
                </Group>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
