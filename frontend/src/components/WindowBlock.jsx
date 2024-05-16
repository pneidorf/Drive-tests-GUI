import React, { useState } from 'react';
import { MapContainer, TileLayer} from 'react-leaflet';
import 'leaflet/dist/leaflet.css';


function WindowBlock() {
  const [activeTab, setActiveTab] = useState('plots');

  const handleTabClick = (tab) => {
    setActiveTab(tab);
  };

  return (
    <div className="window-block">
      <div className="tabs-container">
        <div className="tabs">
          <button className='shadow-gradient-button' onClick={() => handleTabClick('plots')}>Plots</button>
          <button className='shadow-gradient-button' onClick={() => handleTabClick('map')}>Map</button>
          <button className='shadow-gradient-button' onClick={() => handleTabClick('sin')}>Sin</button>
        </div>
      </div>
      <div className='map-block'>
        <div className="map-container">
          {activeTab === 'plots' && 
          <div className='map-block'>
            <div className='map-container-small'>
              Тут будут графики
            </div>  
            <form className='map-container-form'>
              <input placeholder='Bandwith' />
              <input placeholder='...'/>
              <input placeholder='...'/>
              <input placeholder='...'/>
              <input placeholder='...'/>
              <button className='form-button' type='button'>Рассчитать</button>
            </form>
            
            
          </div>}
          {activeTab === 'map' && 
            <MapContainer center={[55.0152, 82.9296]} zoom={13} style={{ height: "570px", width: "100%" }}>
              <TileLayer
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
              />
            </MapContainer>}
          {activeTab === 'sin' && <div>Пустая страница</div>}
        </div>
      </div>
    </div>
  );
}

export default WindowBlock;