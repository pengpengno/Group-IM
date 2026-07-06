import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import App from './App';
import { I18nProvider } from './i18n';
import { store } from './store';

// Create root and render the app
const container = document.getElementById('root');
if (container) {
  const root = ReactDOM.createRoot(container);
  root.render(
    <React.StrictMode>
      <I18nProvider>
        <Provider store={store}>
          <App />
        </Provider>
      </I18nProvider>
    </React.StrictMode>
  );
} else {
  console.error('Failed to find the root element');
}
