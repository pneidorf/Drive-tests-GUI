import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import ProjectDescription from './components/ProjectDescription';
//import MapWindow from './components/MapWindow'
import WindowBlock from './components/WindowBlock';
//import Map from './components/Map';
import Registration from './components/Registration';


class App extends React.Component {
  render () {
    return (
      <div className="App">
      <Header />
      <ProjectDescription />
      {/*<MapWindow />*/}
      <aside>
      <WindowBlock />
      </aside>
      {/*<Map />*/}
    </div>
    )
  }

}

export default App;
