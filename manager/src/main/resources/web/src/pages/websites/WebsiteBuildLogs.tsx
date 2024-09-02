import { Divider, Flex, Group, ScrollArea, Skeleton, Stack, Text } from '@mantine/core';
import { useNavigate, useParams } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { notifications } from '@mantine/notifications';
import Page from '../../components/Page';
import ShortUuid from '../../components/ShortUuid';
import { useIsMobile } from '../../util/mobile';
import BuildLog from '../../types/BuildLog';
import { get, ws as wsocket } from '../../util/api';
import { useDebouncedState } from '../../util/useDebouncedState';
import Duration from '../../components/Duration';
import HeaderWithButton from '../../components/HeaderWithButton';
import StopBuildButton from '../../components/icons/StopBuildButton';
import Error from '../../components/Error';

interface Props {
  websiteId: string
  buildId: string
  status: string
  duration: number
}

function BuildStatus({ websiteId, buildId, status, duration }: Props) {
  const isMobile = useIsMobile();

  const content = (
    <>
      <Text size="lg">Website: <ShortUuid uuid={websiteId} /></Text>
      <Text size="lg">Build: <ShortUuid uuid={buildId} /></Text>
      <Text size="lg">Status: {status.toLowerCase()}</Text>
      <Text size="lg">Duration: {status !== '<unknown>' &&
        <Duration time={duration} status={status} />}</Text>
    </>
  );

  return isMobile ? (
    <Stack align="center" gap={0}>
      {content}
    </Stack>
  ) : (
    <Group justify="space-around">
      {content}
    </Group>
  );
}

export function Component() {
  const { websiteId, buildId } = useParams();
  const navigate = useNavigate();
  // eslint-disable-next-line no-use-before-define
  const [logs, setLogs] = useDebouncedState<BuildLog[]>([], 100);
  const [status, setStatus] = useState('<unknown>');
  const [duration, setDuration] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const logRef = useRef(null as HTMLDivElement);
  const shouldScrollDown = useRef(true);
  const ws = useRef<WebSocket>(null);

  function stopUpdating() {
    if (ws.current) {
      ws.current.close();
    }
  }

  function getDate(timestamp: string): string {
    const d: Date = new Date(parseInt(timestamp, 10));
    const date = d.toISOString().split('T')[0];
    const time = d.toISOString().split('T')[1].split('.')[0];

    return `${date} ${time} GMT`;
  }

  function update(d) {
    setStatus(d.status);
    setDuration(d.duration);
    setLogs((l) => l.concat(d.logs));
  }

  async function loadLogs() {
    try {
      const d = await get(`/api/websites/${websiteId}/builds/${buildId}/logs`);
      if (d.status !== 200) {
        if (d.status === 404) {
          navigate(`/websites/${websiteId}/builds`);
          return;
        }

        setError(d.body.title || 'An error occurred');
        stopUpdating();
        return;
      }

      if (['error', 'stopped', 'deployed'].includes(d.body.status)) {
        stopUpdating();
      }

      update(d.body);

      const finishedStatuses = ['STOPPED', 'ERROR', 'DEPLOYED'];
      if (finishedStatuses.includes(d.body.status)) {
        stopUpdating();
      }

      if (!ws.current && !finishedStatuses.includes(d.body.status)) {
        const stagedLogs = d.body.logs;
        const lastTimestamp = stagedLogs.length !== 0 ?
          stagedLogs[stagedLogs.length - 1].timestamp : 0;

        ws.current = await wsocket(
          `/api/ws/websites/${websiteId}/builds/${buildId}/logs?after=${lastTimestamp}`,
          `WEBSITE_LOGS-${websiteId}-${buildId}`
        );
        ws.current.onerror = () => {
          notifications.show({
            message: 'An error occured with the WebSocket connection. ' +
              'Logs will not automatically update',
            color: 'red'
          });
          ws.current.close();
        };
        ws.current.onmessage = (event) => {
          const eventData = JSON.parse(event.data);
          const { type, data } = eventData;
          // eslint-disable-next-line default-case
          switch (type) {
            case 'logs':
              update(data);
              if (finishedStatuses.includes(data.status)) {
                // Wait 10 seconds for final logs to come through
                setTimeout(() => {
                  ws.current.close();
                }, 10000);
              }
              break;
            case 'error':
              setError(data);
              break;
          }
        };
      }
    } catch (err) {
      setError(err.message);
      stopUpdating();
      setStatus('<unknown>');
      setDuration(0);
    } finally {
      setLoading(false);
    }
  }

  // TODO: Fix scrolling
  function handleScroll({ y }: { y: number }) {
    if (logRef.current === null) return;
    shouldScrollDown.current = logRef.current.scrollHeight -
      y <= logRef.current.clientHeight + 32;
  }

  useEffect(() => {
    if (shouldScrollDown.current) {
      logRef.current?.scrollTo(0, logRef.current?.scrollHeight);
    }
  }, [logs]);

  useEffect(() => {
    loadLogs();

    return () => {
      stopUpdating();
    };
  }, []);

  return (
    <Page title="Build Logs">
      <Flex direction="column" flex={1} style={{
        overflow: 'hidden'
      }}>
        <HeaderWithButton title="Build Logs">
          <StopBuildButton status={status} websiteId={websiteId} buildId={buildId} size="lg" />
        </HeaderWithButton>
        <BuildStatus websiteId={websiteId} buildId={buildId} status={status} duration={duration} />
        <Divider />
        {error ? <Error>{error}</Error> : (
          <ScrollArea viewportRef={logRef} scrollbars="y" p="xs"
            onScrollPositionChange={handleScroll} style={{
              wordBreak: 'break-word'
            }}>
            {loading ? (
              <Stack gap="xs">
                {new Array(10).fill(0).map((_, i) => (
                  // eslint-disable-next-line react/no-array-index-key
                  <Skeleton height="1.25em" width="100%" key={i} />
                ))}
              </Stack>
            ) : (
              <>
                {logs.map((e) => (
                  <Text c={e.type === 'info' ? 'white' : 'red'} key={e.uuid}>
                    <Text span c="gray" fz="sm">
                      {getDate(e.timestamp)}
                    </Text> [{e.type}] {e.log}</Text>
                ))}
              </>
            )}
          </ScrollArea>
        )}
      </Flex>
    </Page>
  );
}
