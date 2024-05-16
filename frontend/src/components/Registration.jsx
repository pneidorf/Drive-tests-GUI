import React, { useState } from 'react';

class Registration extends React.Component {

    render() {
        return (
            <form className='map-container-form'>
              <input placeholder='Введите логин/почту' />
              <input placeholder='Введите пароль'/>
            </form>
        )
    }

}

export default Registration;