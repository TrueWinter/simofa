import { useEffect, useState } from 'react';
import { useActionData, useParams } from 'react-router-dom';
import { Text, Title } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import FormSkeleton from '../../components/FormSkeleton';
import Error from '../../components/Error';
import DeployServerForm from '../../components/forms/DeployServerForm';
import { HttpResponse, get, put } from '../../util/api';
import Page from '../../components/Page';
import useRefresh from '../../util/refresh';
import { scrollToTop } from '../../util/scroll';
import { requestBodyToJson } from '../../util/forms';
import type { DeployServer } from '../../types/java';

export function Component() {
  const { id } = useParams();
  const data = useActionData() as HttpResponse;
  const refresh = useRefresh();
  const [error, setError] = useState(null);
  const [server, setServer] = useState<DeployServer>();

  useEffect(() => {
    get<DeployServer>(`/api/deploy-servers/${id}`).then((resp) => {
      if (resp.success === false) {
        setError(resp.body.title || 'An error occurred');
        return;
      }

      setServer(resp.body);
    }).catch((err) => {
      setError(err);
    });
  }, []);

  useEffect(() => {
    if (data) {
      if (data.status === 200) {
        notifications.show({
          message: 'Website edited'
        });
        refresh();
      } else {
        scrollToTop();
      }
    }
  }, [data]);

  return (
    <Page title="Edit Deploy Server">
      {data && data.status !== 200 && <Text c="red">{data.body.title || 'An error occurred'}</Text>}
      {error ? <Error>{error}</Error> : (
        server ? <DeployServerForm server={server} /> : (
          <>
            <Title>Edit Deploy Server</Title>
            <FormSkeleton />
          </>
        )
      )}
    </Page>
  );
}

export async function action({ request, params }) {
  return put(`/api/deploy-servers/${params.id}`, await requestBodyToJson(request));
}
