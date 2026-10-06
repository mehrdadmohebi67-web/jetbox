/* Jet Box Cloud / Mission / GPS safety patch
 * Build: 30.1.1.1.4 -> hotfix 30.1.1.1.5
 */
(function(){
  'use strict';
  function jbFixNow(){ return new Date().toISOString(); }
  try{
    if(typeof window.jbCloudInit === 'function' && !window.__jbCloudInitMissionGpsFix){
      const originalInit=window.jbCloudInit;
      window.jbCloudInit=async function(){
        const st=window.JB_CLOUD_STATE||{};
        const hadDirty=!!st.dirty;
        let localSnapshot=null;
        if(hadDirty&&window.db){try{localSnapshot=JSON.parse(JSON.stringify(window.db));}catch(e){}}
        const result=await originalInit.apply(this,arguments);
        if(hadDirty&&localSnapshot&&window.db){
          try{
            const merge=typeof window.jbCloudMerge==='function'?window.jbCloudMerge(window.db,localSnapshot):localSnapshot;
            window.db=merge;
            try{localStorage.setItem('jetbox_db',JSON.stringify(window.db));}catch(e){}
            if(window.JB_CLOUD_STATE)window.JB_CLOUD_STATE.dirty=true;
            if(typeof window.jbCloudSchedulePush==='function')window.jbCloudSchedulePush();
          }catch(e){console.warn('Jet Box hydration preservation skipped',e)}
        }
        return result;
      };
      window.__jbCloudInitMissionGpsFix=true;
    }
  }catch(e){console.warn('Jet Box cloud-init patch skipped',e)}
  try{
    if(typeof window.jbMissionSend==='function'&&!window.__jbMissionSendFix){
      const originalSend=window.jbMissionSend;
      window.jbMissionSend=function(missionId,courierId){
        const m=Array.isArray(window.db?.courierMissions)?window.db.courierMissions.find(x=>String(x?.missionId||'')===String(missionId||'')):null;
        if(m){const target=String(courierId||m.courierTarget||'').trim();if(target)m.courierTarget=target;m.updatedAt=jbFixNow();if(typeof window.jbMissionNotifyCourier==='function'&&m.courierTarget){try{window.jbMissionNotifyCourier(m)}catch(e){}}}
        const out=originalSend.apply(this,arguments);
        try{if(out){out.courierTarget=String(out.courierTarget||courierId||'').trim()||null;out.updatedAt=jbFixNow()}if(typeof window.saveDB==='function')window.saveDB();if(typeof window.jbCloudSchedulePush==='function')window.jbCloudSchedulePush()}catch(e){console.warn('Jet Box mission cloud queue skipped',e)}
        return out;
      };
      window.__jbMissionSendFix=true;
    }
  }catch(e){console.warn('Jet Box mission patch skipped',e)}
  try{
    if(typeof window.jbMissionNotifyCourier==='function'&&!window.__jbMissionNotifyFix){
      const originalNotify=window.jbMissionNotifyCourier;
      window.jbMissionNotifyCourier=function(m){
        if(!m||!window.db)return false;
        const target=String(m.courierTarget||m.courierId||'').trim();if(!target)return false;m.courierTarget=target;
        const ok=originalNotify.apply(this,[m]);
        try{const ns=Array.isArray(window.db.notifications)?window.db.notifications:[];ns.filter(n=>n&&String(n.type||'')==='مأموریت پیک'&&String(n.missionId||'')===String(m.missionId||'')).forEach(n=>{n.courierId=target;n.courierKey=target})}catch(e){}
        return ok;
      };
      window.__jbMissionNotifyFix=true;
    }
  }catch(e){console.warn('Jet Box mission notification patch skipped',e)}
  try{
    if(typeof window.jbCloudPushCourierLocation==='function'&&!window.__jbGpsPushFix){
      const originalGpsPush=window.jbCloudPushCourierLocation;
      window.jbCloudPushCourierLocation=async function(rec){
        if(!rec)return false;
        try{const ok=await originalGpsPush.apply(this,arguments);if(ok)return true}catch(e){}
        try{
          if(typeof window.ensureCourierLocations==='function')window.ensureCourierLocations();
          const id=String(rec.courierId||rec.id||'').trim();
          if(id&&window.db){const list=Array.isArray(window.db.courierLocations)?window.db.courierLocations:[];const i=list.findIndex(x=>String(x?.courierId||x?.id||'')===id);const next=Object.assign({},i>=0?list[i]:{},rec,{courierId:id,updatedAt:rec.updatedAt||jbFixNow()});if(i>=0)list[i]=next;else list.push(next);window.db.courierLocations=list;if(typeof window.saveDB==='function')window.saveDB()}
        }catch(e){console.warn('Jet Box legacy GPS fallback skipped',e)}
        return false;
      };
      window.__jbGpsPushFix=true;
    }
  }catch(e){console.warn('Jet Box GPS patch skipped',e)}
  try{
    if(typeof window.saveUser==='function'&&!window.__jbNewCourierSyncFix){
      const originalSaveUser=window.saveUser;
      window.saveUser=function(){
        const result=originalSaveUser.apply(this,arguments);
        try{if(window.role==='مدیریت'&&typeof window.jbNormalizeCourierIdentityStore==='function'){window.jbNormalizeCourierIdentityStore(window.db);if(typeof window.saveDB==='function')window.saveDB();if(typeof window.jbCloudSchedulePush==='function')window.jbCloudSchedulePush()}}catch(e){console.warn('Jet Box new courier cloud sync skipped',e)}
        return result;
      };
      window.__jbNewCourierSyncFix=true;
    }
  }catch(e){console.warn('Jet Box new-courier patch skipped',e)}
  console.info('Jet Box hotfix 30.1.1.1.5 loaded: mission/cloud/GPS protection active');
})();
