import { nprogress } from '@mantine/nprogress';
import { router } from '../router';

let redirectUrl = '/';
export function redirectAfterLogin() {
  return redirectUrl;
}

interface Opts {
  noRedirectOn401?: boolean
  showProgress?: boolean
}

interface RequestBase extends Opts {
    route: string
}

const METHODS_THAT_CANNOT_CONTAIN_DATA = ['GET', 'DELETE'] as const;
interface RequestWithoutData extends RequestBase {
    method: typeof METHODS_THAT_CANNOT_CONTAIN_DATA[number]
}

const METHODS_THAT_CAN_CONTAIN_DATA = ['POST', 'PUT', 'PATCH'] as const;
interface RequestWithData extends RequestBase {
    method: typeof METHODS_THAT_CAN_CONTAIN_DATA[number],
    data?: FormData | object
}

type Request = RequestWithData | RequestWithoutData

const SUCCESS_STATUS_CODES = [200, 204];

/*
  To check if a response is an error response, check
  `if (resp.success === false) {}`. Yes, you did read
  that right. Due to a weird TypeScript issue,
  `!resp.success` will not work.
 */
interface ErrorResponse {
  status: number
  success: false
  body: {
    title: string
  }
}

interface SuccessResponse<T> {
  status: number
  success: true
  body: T
}

type Response<T> = SuccessResponse<T> | ErrorResponse

export type HttpResponse<T = any> = Response<T> & {
  status: number
}

// eslint-disable-next-line no-underscore-dangle
async function _request(opts: Request): Promise<HttpResponse> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json'
  };

  const jwt = localStorage.getItem('token');
  if (jwt) headers.Authorization = `Bearer ${jwt}`;

  let serializedData: string;

  // METHODS_THAT_CAN_CONTAIN_DATA.includes(request.method) would be better, but TypeScript wouldn't allow it
  if (opts.method === 'POST' || opts.method === 'PUT' || opts.method === 'PATCH') {
    if (!opts.data) {
      serializedData = undefined;
    } else if (opts.data instanceof FormData) {
      serializedData = JSON.stringify(Object.fromEntries(opts.data));
    } else {
      serializedData = JSON.stringify(opts.data);
    }
  }

  const resp = await fetch(opts.route, {
    method: opts.method,
    headers,
    body: serializedData
  });

  if (resp.status === 401 && !opts.noRedirectOn401) {
    localStorage.removeItem('token');
    redirectUrl = location.pathname;
    // https://github.com/remix-run/react-router/issues/9422#issuecomment-1301182219
    router.navigate('/login');
    return {
      status: 401,
      success: false,
      body: {
        title: 'Unauthorized'
      }
    };
  }

  const respData = await resp.text();
  return {
    status: resp.status,
    success: SUCCESS_STATUS_CODES.includes(resp.status),
    body: respData ? JSON.parse(respData) : {}
  };
}

async function request<T>(opts: Request): Promise<HttpResponse<T>> {
  try {
    if (opts.showProgress) nprogress.start();
    return _request(opts);
  } finally {
    if (opts.showProgress) nprogress.complete();
  }
}

export async function get<T = any>(route: string, opts?: Opts) {
  return request<T>({
    method: 'GET',
    route,
    ...opts
  });
}

export async function post<T = any>(route: string, data: FormData | object, opts?: Opts) {
  return request<T>({
    method: 'POST',
    route,
    data,
    ...opts
  });
}

export async function patch<T = any>(route: string, data: FormData | object, opts?: Opts) {
  return request<T>({
    method: 'PATCH',
    route,
    data,
    ...opts
  });
}

export async function put<T = any>(route: string, data: FormData | object, opts?: Opts) {
  return request<T>({
    method: 'PUT',
    route,
    data,
    ...opts
  });
}

export async function del<T = any>(route: string, opts?: Opts) {
  return request<T>({
    method: 'DELETE',
    route,
    ...opts
  });
}

export async function ws(route: string, room: string, opts?: Opts) {
  const resp = await post('/api/login/ws', {
    roomId: room
  }, opts);

  if (resp.success === false) {
    return;
  }

  // Some browsers don't fully support relative URLs in WebSockets.
  const host = `${location.protocol === 'http:' ? 'ws:' : 'wss:'}//${location.host}`;
  const url = new URL(route, host);
  url.searchParams.set('token', resp.body.token);
  return new WebSocket(url.href);
}

function getJWT(): Record<string, any> | null {
  const jwt = localStorage.getItem('token');
  return jwt ? JSON.parse(atob(jwt.split('.')[1])) : null;
}

export function getUserId(): string {
  return getJWT()?.user_id || null;
}
