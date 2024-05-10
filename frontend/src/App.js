import React from 'react';
import Header from './components/Header';
import ProjectDescription from './components/ProjectDescription';
//import MapWindow from './components/MapWindow'
import WindowBlock from './components/WindowBlock';
//import Map from './components/Map';


class App extends React.Component {
  render () {
    return (
      <div className="App">
      <Header />
      <ProjectDescription />
      {/*<MapWindow />*/}
      <WindowBlock />
      {/*<Map />*/}
    </div>
    )
  }

}

export default App;
