import { useEffect, useState } from 'react';
import { useActionData, useParams } from 'react-router-dom';
import { Text, Title } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import FormSkeleton from '../../components/FormSkeleton';
import Error from '../../components/Error';
import { HttpResponse, get, put } from '../../util/api';
import Page from '../../components/Page';
import useRefresh from '../../util/refresh';
import { scrollToTop } from '../../util/scroll';
import { requestBodyToJson } from '../../util/forms';
import type { GitCredential } from '../../types/java';
import GitCredentialsForm from '../../components/forms/GitCredentialsForm';

export function Component() {
  const { id } = useParams();
  const data = useActionData() as HttpResponse;
  const refresh = useRefresh();
  const [error, setError] = useState(null);
  const [credentials, setCredentials] = useState<GitCredential>();

  useEffect(() => {
    get<GitCredential>(`/api/git-credentials/${id}`).then((resp) => {
      if (resp.success === false) {
        setError(resp.body.title || 'An error occurred');
        return;
      }

      setCredentials(resp.body);
    }).catch((err) => {
      setError(err);
    });
  }, []);

  useEffect(() => {
    if (data) {
      if (data.status === 200) {
        notifications.show({
          message: 'Git credentials edited'
        });
        refresh();
      } else {
        scrollToTop();
      }
    }
  }, [data]);

  return (
    <Page title="Edit Git Credentials">
      {data && data.status !== 200 && <Text c="red">{data.body.title || 'An error occurred'}</Text>}
      {error ? <Error>{error}</Error> : (
        credentials ? <GitCredentialsForm git={credentials} /> : (
          <>
            <Title>Edit Git Credentials</Title>
            <FormSkeleton />
          </>
        )
      )}
    </Page>
  );
}

export async function action({ request, params }) {
  const body = await requestBodyToJson(request);

  if (!body.password || !body.confirmPassword) {
    body.password = null;
    body.confirmPassword = null;
  }

  return put(`/api/git-credentials/${params.id}`, body);
}
