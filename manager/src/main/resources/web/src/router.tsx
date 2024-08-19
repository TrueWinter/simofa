import { Navigate, createBrowserRouter } from 'react-router-dom';
import ErrorPage from './ErrorPage';
import Refresh from './pages/Refresh';

export const router = createBrowserRouter([{
  lazy: () => import('./RootLayout'),
  errorElement: <ErrorPage />,
  children: [{
    path: '/',
    element: <Navigate to="/websites" />
  }, {
    path: '/refresh',
    element: <Refresh />
  }, {
    path: '/login',
    lazy: () => import('./pages/Login')
  }, {
    path: '/logout',
    lazy: () => import('./pages/Logout')
  }, {
    path: '/websites',
    lazy: () => import('./pages/websites/Websites')
  }, {
    path: '/websites/add',
    lazy: () => import('./pages/websites/AddWebsite')
  }, {
    path: '/websites/:id/edit',
    lazy: () => import('./pages/websites/EditWebsite')
  }, {
    path: '/websites/:id/builds',
    lazy: () => import('./pages/websites/WebsiteBuilds')
  }, {
    path: '/websites/:websiteId/builds/:buildId/logs',
    lazy: () => import('./pages/websites/WebsiteBuildLogs')
  }, {
    path: '/deploy-servers',
    lazy: () => import('./pages/deploy-servers/DeployServers')
  }, {
    path: '/deploy-servers/add',
    lazy: () => import('./pages/deploy-servers/AddDeployServer')
  }, {
    path: '/deploy-servers/:id/edit',
    lazy: () => import('./pages/deploy-servers/EditDeployServer')
  }, {
    path: '/accounts',
    lazy: () => import('./pages/accounts/Accounts')
  }, {
    path: '/accounts/add',
    lazy: () => import('./pages/accounts/AddAccount')
  }, {
    path: '/accounts/:id/edit',
    lazy: () => import('./pages/accounts/EditAccount')
  }, {
    path: '/builds',
    lazy: () => import('./pages/builds/Builds')
  }, {
    path: '/git/credentials',
    lazy: () => import('./pages/git/credentials/GitCredentials')
  }, {
    path: '/git/credentials/add',
    lazy: () => import('./pages/git/credentials/AddGitCredentials')
  }, {
    path: '/git/credentials/:id/edit',
    lazy: () => import('./pages/git/credentials/EditGitCredentials')
  }, {
    path: '/git/github-app',
    lazy: () => import('./pages/git/github-app/GitHubApp')
  }]
}]);
